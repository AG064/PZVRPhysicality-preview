//! Bounded rigid-body solver. Game inventory and renderer objects never enter Rust.
use rapier3d::na::{Quaternion, UnitQuaternion};
use rapier3d::prelude::*;
use std::collections::BTreeMap;

mod bridge;
mod concave;
const BODY_LIMIT: usize = 512;
type Result<T> = std::result::Result<T, &'static str>;

fn finite(values: &[f32], max: f32) -> Result<()> {
    if values.iter().any(|v| !v.is_finite() || v.abs() > max) {
        Err("Nonfinite or oversized physics input")
    } else {
        Ok(())
    }
}

fn pose(values: &[f32]) -> Result<Isometry<f32>> {
    if values.len() != 7 {
        return Err("Pose requires seven floats");
    }
    finite(&values[..3], 100_000.)?;
    finite(&values[3..], 1.)?;
    let q = Quaternion::new(values[6], values[3], values[4], values[5]);
    if !(0.99..=1.01).contains(&q.norm_squared()) {
        return Err("Pose quaternion must be normalized");
    }
    Ok(Isometry::from_parts(
        Translation::new(values[0], values[1], values[2]),
        UnitQuaternion::new_normalize(q),
    ))
}

pub struct World {
    pipeline: PhysicsPipeline,
    islands: IslandManager,
    broad: BroadPhaseBvh,
    narrow: NarrowPhase,
    bodies: RigidBodySet,
    colliders: ColliderSet,
    joints: ImpulseJointSet,
    multibody: MultibodyJointSet,
    ccd: CCDSolver,
    ids: BTreeMap<u64, RigidBodyHandle>,
    joint_ids: BTreeMap<u64, ImpulseJointHandle>,
    next: u64,
    springs: BTreeMap<u64, Spring>,
}
struct Spring {
    target: RigidBodyHandle,
    joint: ImpulseJointHandle,
}
impl Default for World {
    fn default() -> Self {
        Self {
            pipeline: PhysicsPipeline::new(),
            islands: IslandManager::new(),
            broad: BroadPhaseBvh::new(),
            narrow: NarrowPhase::new(),
            bodies: RigidBodySet::new(),
            colliders: ColliderSet::new(),
            joints: ImpulseJointSet::new(),
            multibody: MultibodyJointSet::new(),
            ccd: CCDSolver::new(),
            ids: BTreeMap::new(),
            joint_ids: BTreeMap::new(),
            next: 1,
            springs: BTreeMap::new(),
        }
    }
}
impl World {
    fn handle(&self, id: u64) -> Result<RigidBodyHandle> {
        self.ids.get(&id).copied().ok_or("Unknown body handle")
    }
    fn insert(
        &mut self,
        kind: i32,
        transform: Isometry<f32>,
        mass: f32,
        shape: SharedShape,
    ) -> Result<u64> {
        if self.bodies.len() >= BODY_LIMIT || !mass.is_finite() || !(0.001..=200.).contains(&mass) {
            return Err("Body budget or mass");
        }
        let builder = match kind {
            0 => RigidBodyBuilder::fixed(),
            1 => RigidBodyBuilder::dynamic(),
            2 => RigidBodyBuilder::kinematic_position_based(),
            _ => return Err("Unknown body kind"),
        };
        let body = builder
            .pose(transform)
            .ccd_enabled(true)
            .linear_damping(0.08)
            .angular_damping(0.15)
            .build();
        let handle = self.bodies.insert(body);
        self.colliders.insert_with_parent(
            ColliderBuilder::new(shape)
                .mass(mass)
                .friction(0.6)
                .restitution(0.08),
            handle,
            &mut self.bodies,
        );
        let id = self.next;
        self.next += 1;
        self.ids.insert(id, handle);
        Ok(id)
    }
    pub fn boxes(&mut self, kind: i32, transform: &[f32], mass: f32, boxes: &[f32]) -> Result<u64> {
        let transform = pose(transform)?;
        if boxes.is_empty() || !boxes.len().is_multiple_of(10) || boxes.len() > 640 {
            return Err("Compound box budget");
        }
        finite(boxes, if kind == 0 { 32. } else { 4. })?;
        let mut shapes = Vec::new();
        for b in boxes.as_chunks::<10>().0 {
            if b[..3]
                .iter()
                .any(|v| *v < 0.001 || *v > if kind == 0 { 32. } else { 2. })
            {
                return Err("Invalid box half extent");
            }
            let local = pose(&b[3..10])?;
            shapes.push((local, SharedShape::cuboid(b[0], b[1], b[2])));
        }
        self.insert(kind, transform, mass, SharedShape::compound(shapes))
    }
    pub fn mesh(
        &mut self,
        kind: i32,
        transform: &[f32],
        mass: f32,
        points: &[f32],
        indices: &[i32],
    ) -> Result<u64> {
        let transform = pose(transform)?;
        if points.len() < 12
            || !points.len().is_multiple_of(3)
            || points.len() > 96_000
            || indices.len() < 3
            || !indices.len().is_multiple_of(3)
            || indices.len() > 192_000
        {
            return Err("Mesh geometry budget");
        }
        finite(points, if kind == 0 { 16. } else { 4. })?;
        let vertices: Vec<Point<f32>> = points
            .as_chunks::<3>()
            .0
            .iter()
            .map(|p| point![p[0], p[1], p[2]])
            .collect();
        if indices
            .iter()
            .any(|i| *i < 0 || *i as usize >= vertices.len())
        {
            return Err("Triangle index outside mesh");
        }
        let faces: Vec<[u32; 3]> = indices
            .as_chunks::<3>()
            .0
            .iter()
            .map(|t| [t[0] as u32, t[1] as u32, t[2] as u32])
            .collect();
        let shape = if kind == 0 {
            SharedShape::trimesh(vertices, faces).map_err(|_| "Invalid static triangle mesh")?
        } else {
            concave::shape(points, indices)?
        };
        self.insert(kind, transform, mass, shape)
    }
    pub fn remove(&mut self, id: u64) -> Result<()> {
        let handle = self.handle(id)?;
        self.bodies.remove(
            handle,
            &mut self.islands,
            &mut self.colliders,
            &mut self.joints,
            &mut self.multibody,
            true,
        );
        self.release_spring(id)?;
        self.ids.remove(&id);
        self.joint_ids
            .retain(|_, handle| self.joints.contains(*handle));
        Ok(())
    }
    pub fn prepare_mesh(&self, points: &[f32], indices: &[i32]) -> Result<i32> {
        concave::prepare(points, indices)
    }
    pub fn prepared_mesh(
        &mut self,
        transform: &[f32],
        mass: f32,
        points: &[f32],
        indices: &[i32],
    ) -> Result<Option<u64>> {
        let transform = pose(transform)?;
        let Some(shape) = concave::prepared(points, indices)? else {
            return Ok(None);
        };
        self.insert(1, transform, mass, shape).map(Some)
    }
    pub fn drive(&mut self, id: u64, transform: &[f32]) -> Result<()> {
        let transform = pose(transform)?;
        let handle = self.handle(id)?;
        let body = &mut self.bodies[handle];
        if !body.is_kinematic() {
            return Err("Drive requires a kinematic target");
        }
        body.set_next_kinematic_position(transform);
        Ok(())
    }
    /// Explicit groups also filter swept CCD pairs, unlike joint contact flags alone.
    pub fn groups(&mut self, id: u64, membership: u32, filter: u32) -> Result<()> {
        if membership == 0 || filter == 0 {
            return Err("Empty collision group");
        }
        let handle = self.handle(id)?;
        let colliders = self.bodies[handle].colliders().to_vec();
        let groups = InteractionGroups::new(
            Group::from_bits_retain(membership),
            Group::from_bits_retain(filter),
        );
        for collider in colliders {
            self.colliders[collider].set_collision_groups(groups);
        }
        self.bodies[handle].wake_up(true);
        Ok(())
    }
    pub fn distance(&self, id: u64, coordinates: &[f32]) -> Result<f32> {
        if coordinates.len() != 3 {
            return Err("Surface query point size");
        }
        finite(coordinates, 100_000.)?;
        let body = &self.bodies[self.handle(id)?];
        let point = point![coordinates[0], coordinates[1], coordinates[2]];
        let mut distance = f32::INFINITY;
        for handle in body.colliders() {
            let collider = &self.colliders[*handle];
            let projected = collider
                .shape()
                .project_point(collider.position(), &point, true);
            distance = distance.min((projected.point - point).norm());
        }
        if !distance.is_finite() {
            return Err("Body has no queryable collider");
        }
        Ok(distance)
    }
    pub fn raycast(&self, values: &[f32], excluded: &[u64]) -> Result<Option<[f32; 7]>> {
        if values.len() != 7 || excluded.len() > 256 {
            return Err("Ray query extent");
        }
        finite(&values[..3], 100_000.)?;
        finite(&values[3..], 1000.)?;
        let direction = vector![values[3], values[4], values[5]];
        if !(0.99..=1.01).contains(&direction.norm_squared()) || values[6] <= 0. || values[6] > 8. {
            return Err("Ray query direction or range");
        }
        let ignored: std::collections::HashSet<_> = excluded
            .iter()
            .filter_map(|id| self.ids.get(id).copied())
            .collect();
        let ray = Ray::new(point![values[0], values[1], values[2]], direction);
        let mut nearest = values[6];
        let mut result = None;
        for (_, collider) in self.colliders.iter() {
            if collider
                .parent()
                .is_some_and(|parent| ignored.contains(&parent))
            {
                continue;
            }
            if let Some(hit) =
                collider
                    .shape()
                    .cast_ray_and_get_normal(collider.position(), &ray, nearest, true)
            {
                let at = ray.point_at(hit.time_of_impact);
                nearest = hit.time_of_impact;
                result = Some([
                    nearest,
                    at.x,
                    at.y,
                    at.z,
                    hit.normal.x,
                    hit.normal.y,
                    hit.normal.z,
                ]);
            }
        }
        Ok(result)
    }
    pub fn capsule_sweep(&self, values: &[f32], excluded: &[u64]) -> Result<Option<[f32; 4]>> {
        use rapier3d::parry::bounding_volume::BoundingVolume;
        use rapier3d::parry::query::{ShapeCastOptions, cast_shapes};
        if values.len() != 9 || excluded.len() > 256 {
            return Err("Capsule query extent");
        }
        finite(&values[..3], 100_000.)?;
        finite(&values[3..], 1000.)?;
        let direction = vector![values[3], values[4], values[5]];
        let (radius, height, range) = (values[6], values[7], values[8]);
        if !(0.99..=1.01).contains(&direction.norm_squared())
            || !(0.05..=0.4).contains(&radius)
            || height < radius * 2.
            || height > 2.25
            || range <= 0.
            || range > 2.
        {
            return Err("Capsule query dimensions, direction or range");
        }
        let shape = SharedShape::capsule_z(height * 0.5 - radius, radius);
        let start = Isometry::translation(values[0], values[1], values[2] + height * 0.5);
        let end = Isometry::translation(
            values[0] + direction.x * range,
            values[1] + direction.y * range,
            start.translation.z + direction.z * range,
        );
        let swept = shape.compute_aabb(&start).merged(&shape.compute_aabb(&end));
        let ignored: std::collections::HashSet<_> = excluded
            .iter()
            .filter_map(|id| self.ids.get(id).copied())
            .collect();
        let mut nearest = range;
        let mut result = None;
        for (_, collider) in self.colliders.iter() {
            if collider
                .parent()
                .is_some_and(|parent| ignored.contains(&parent))
                || !collider.compute_aabb().intersects(&swept)
            {
                continue;
            }
            let options = ShapeCastOptions {
                max_time_of_impact: nearest,
                stop_at_penetration: false,
                compute_impact_geometry_on_penetration: true,
                ..Default::default()
            };
            let hit = cast_shapes(
                &start,
                &direction,
                shape.as_ref(),
                collider.position(),
                &Vector::zeros(),
                collider.shape(),
                options,
            )
            .map_err(|_| "Unsupported capsule contact shape")?;
            if let Some(hit) = hit {
                let normal = collider.position().rotation * hit.normal2.into_inner();
                if !hit.time_of_impact.is_finite() || normal.iter().any(|value| !value.is_finite())
                {
                    return Err("Invalid capsule contact result");
                }
                if direction.dot(&normal) >= -0.00001 {
                    continue;
                }
                nearest = hit.time_of_impact;
                result = Some([nearest, normal.x, normal.y, normal.z]);
            }
        }
        Ok(result)
    }
    pub fn velocity(&mut self, id: u64, linear: &[f32], angular: &[f32]) -> Result<()> {
        if linear.len() != 3 || angular.len() != 3 {
            return Err("Velocity vector size");
        }
        finite(linear, 20.)?;
        finite(angular, 30.)?;
        let handle = self.handle(id)?;
        self.bodies[handle].set_linvel(vector![linear[0], linear[1], linear[2]], true);
        self.bodies[handle].set_angvel(vector![angular[0], angular[1], angular[2]], true);
        // Rapier's velocity setters do not wake when both requested velocities are zero.
        // Explicit sleep changes may still be pending when an idle world skipped its step.
        self.bodies[handle].wake_up(true);
        self.islands.wake_up(&mut self.bodies, handle, true);
        Ok(())
    }
    /// Restore a saved dynamic object without moving its pose or making it settle again.
    pub fn sleep(&mut self, id: u64) -> Result<()> {
        let handle = self.handle(id)?;
        let body = &mut self.bodies[handle];
        if !body.is_dynamic() {
            return Err("Sleep requires dynamic body");
        }
        body.sleep();
        Ok(())
    }
    /// Counterbalanced sliders remain dynamic contact bodies; no pose teleport is used.
    pub fn gravity_scale(&mut self, id: u64, scale: f32) -> Result<()> {
        if !scale.is_finite() || !(0. ..=1.).contains(&scale) {
            return Err("Gravity scale bounds");
        }
        let handle = self.handle(id)?;
        let body = &mut self.bodies[handle];
        if !body.is_dynamic() {
            return Err("Gravity scale requires dynamic body");
        }
        body.set_gravity_scale(scale, true);
        Ok(())
    }
    pub fn spring(&mut self, id: u64, values: &[f32]) -> Result<()> {
        if values.len() != 13 {
            return Err("Spring input extent");
        }
        let target = pose(&values[..7])?;
        finite(&values[7..], 5000.)?;
        if values[7..].iter().any(|v| *v < 0.) || values[9] > 500. || values[12] > 50. {
            return Err("Spring force or torque limit");
        }
        let handle = self.handle(id)?;
        if !self.bodies[handle].is_dynamic() {
            return Err("Spring requires a dynamic body");
        }
        if !self.springs.contains_key(&id) {
            if self.springs.len() >= 16
                || self.bodies.len() >= BODY_LIMIT
                || self.joints.len() >= BODY_LIMIT
            {
                return Err("Spring target budget");
            }
            let parent = self.bodies.insert(
                RigidBodyBuilder::kinematic_position_based()
                    .pose(target)
                    .build(),
            );
            let joint = self.joints.insert(
                parent,
                handle,
                GenericJointBuilder::new(JointAxesMask::empty()).contacts_enabled(false),
                true,
            );
            self.springs.insert(
                id,
                Spring {
                    target: parent,
                    joint,
                },
            );
        }
        let spring = &self.springs[&id];
        let joint = self
            .joints
            .get_mut(spring.joint, true)
            .ok_or("Stale spring joint")?;
        for (axis, offset) in [
            (JointAxis::LinX, 7),
            (JointAxis::LinY, 7),
            (JointAxis::LinZ, 7),
            (JointAxis::AngX, 10),
            (JointAxis::AngY, 10),
            (JointAxis::AngZ, 10),
        ] {
            joint
                .data
                .set_motor_model(axis, MotorModel::ForceBased)
                .set_motor_position(axis, 0., values[offset], values[offset + 1]);
            joint.data.set_motor_max_force(axis, values[offset + 2]);
        }
        self.bodies[spring.target].set_next_kinematic_position(target);
        Ok(())
    }
    pub fn release_spring(&mut self, id: u64) -> Result<()> {
        self.handle(id)?;
        if let Some(spring) = self.springs.remove(&id) {
            self.bodies.remove(
                spring.target,
                &mut self.islands,
                &mut self.colliders,
                &mut self.joints,
                &mut self.multibody,
                true,
            );
        }
        Ok(())
    }
    fn joint(&mut self, a: u64, b: u64, data: impl Into<GenericJoint>) -> Result<u64> {
        if a == b || self.joint_ids.len() >= BODY_LIMIT {
            return Err("Joint budget or self joint");
        }
        let handle = self
            .joints
            .insert(self.handle(a)?, self.handle(b)?, data, true);
        let id = self.next;
        self.next += 1;
        self.joint_ids.insert(id, handle);
        Ok(id)
    }
    pub fn pin(&mut self, a: u64, b: u64, local: &[f32]) -> Result<u64> {
        self.joint(
            a,
            b,
            FixedJointBuilder::new()
                .local_frame1(pose(local)?)
                .contacts_enabled(true),
        )
    }
    #[allow(clippy::too_many_arguments)] // Explicit anchors match the numeric JNI protocol.
    pub fn hinge(
        &mut self,
        a: u64,
        b: u64,
        anchor_a: &[f32],
        anchor_b: &[f32],
        axis: &[f32],
        low: f32,
        high: f32,
    ) -> Result<u64> {
        if anchor_a.len() != 3
            || anchor_b.len() != 3
            || axis.len() != 3
            || !low.is_finite()
            || !high.is_finite()
            || low >= high
            || low < -std::f32::consts::PI
            || high > std::f32::consts::PI
        {
            return Err("Hinge inputs");
        }
        finite(anchor_a, 4.)?;
        finite(anchor_b, 4.)?;
        finite(axis, 1.)?;
        let axis =
            UnitVector::try_new(vector![axis[0], axis[1], axis[2]], 0.001).ok_or("Hinge axis")?;
        self.joint(
            a,
            b,
            RevoluteJointBuilder::new(axis)
                .local_anchor1(point![anchor_a[0], anchor_a[1], anchor_a[2]])
                .local_anchor2(point![anchor_b[0], anchor_b[1], anchor_b[2]])
                .limits([low, high])
                .contacts_enabled(false),
        )
    }
    pub fn motor(
        &mut self,
        id: u64,
        target: f32,
        stiffness: f32,
        damping: f32,
        force: f32,
    ) -> Result<()> {
        finite(&[target, stiffness, damping, force], 1000.)?;
        if !(0. ..=100.).contains(&force) || stiffness < 0. || damping < 0. {
            return Err("Hinge motor bounds");
        }
        let handle = *self.joint_ids.get(&id).ok_or("Unknown joint handle")?;
        let joint = self
            .joints
            .get_mut(handle, true)
            .ok_or("Stale joint handle")?;
        let hinge = joint.data.as_revolute_mut().ok_or("Motor requires hinge")?;
        let limits = hinge.limits().ok_or("Hinge limits missing")?;
        if target < limits.min || target > limits.max {
            return Err("Motor target outside hinge limits");
        }
        hinge
            .set_motor_model(MotorModel::ForceBased)
            .set_motor_position(target, stiffness, damping)
            .set_motor_max_force(force);
        self.bodies[joint.body1].wake_up(true);
        self.bodies[joint.body2].wake_up(true);
        Ok(())
    }
    pub fn angle(&self, id: u64) -> Result<f32> {
        let joint = self
            .joints
            .get(*self.joint_ids.get(&id).ok_or("Unknown joint handle")?)
            .ok_or("Stale joint handle")?;
        let hinge = joint.data.as_revolute().ok_or("Angle requires hinge")?;
        Ok(hinge.angle(
            self.bodies[joint.body1].rotation(),
            self.bodies[joint.body2].rotation(),
        ))
    }
    #[allow(clippy::too_many_arguments)] // Explicit anchors match the bounded JNI protocol.
    pub fn slider(
        &mut self,
        a: u64,
        b: u64,
        anchor_a: &[f32],
        anchor_b: &[f32],
        axis: &[f32],
        low: f32,
        high: f32,
    ) -> Result<u64> {
        if anchor_a.len() != 3
            || anchor_b.len() != 3
            || axis.len() != 3
            || !low.is_finite()
            || !high.is_finite()
            || low >= high
            || low < -2.
            || high > 2.
        {
            return Err("Slider inputs");
        }
        finite(anchor_a, 4.)?;
        finite(anchor_b, 4.)?;
        finite(axis, 1.)?;
        let axis =
            UnitVector::try_new(vector![axis[0], axis[1], axis[2]], 0.001).ok_or("Slider axis")?;
        self.joint(
            a,
            b,
            PrismaticJointBuilder::new(axis)
                .local_anchor1(point![anchor_a[0], anchor_a[1], anchor_a[2]])
                .local_anchor2(point![anchor_b[0], anchor_b[1], anchor_b[2]])
                .limits([low, high])
                .contacts_enabled(false),
        )
    }
    pub fn slide_motor(
        &mut self,
        id: u64,
        target: f32,
        stiffness: f32,
        damping: f32,
        force: f32,
    ) -> Result<()> {
        finite(&[target, stiffness, damping, force], 1000.)?;
        if !(0. ..=100.).contains(&force) || stiffness < 0. || damping < 0. {
            return Err("Slider motor bounds");
        }
        let handle = *self.joint_ids.get(&id).ok_or("Unknown joint handle")?;
        let joint = self
            .joints
            .get_mut(handle, true)
            .ok_or("Stale joint handle")?;
        let slider = joint
            .data
            .as_prismatic_mut()
            .ok_or("Motor requires slider")?;
        let limits = slider.limits().ok_or("Slider limits missing")?;
        if target < limits.min || target > limits.max {
            return Err("Motor target outside slider limits");
        }
        slider
            .set_motor_model(MotorModel::ForceBased)
            .set_motor_position(target, stiffness, damping)
            .set_motor_max_force(force);
        self.bodies[joint.body1].wake_up(true);
        self.bodies[joint.body2].wake_up(true);
        Ok(())
    }
    pub fn travel(&self, id: u64) -> Result<f32> {
        let joint = self
            .joints
            .get(*self.joint_ids.get(&id).ok_or("Unknown joint handle")?)
            .ok_or("Stale joint handle")?;
        let slider = joint.data.as_prismatic().ok_or("Travel requires slider")?;
        let parent = &self.bodies[joint.body1];
        let child = &self.bodies[joint.body2];
        let axis = parent.rotation() * slider.local_axis1();
        let offset =
            child.position() * slider.local_anchor2() - parent.position() * slider.local_anchor1();
        Ok(axis.dot(&offset))
    }
    pub fn step(&mut self, seconds: f32) -> Result<()> {
        if !seconds.is_finite() || seconds < 0. {
            return Err("Step duration");
        }
        if seconds == 0. {
            return Ok(());
        }
        let duration = seconds.min(0.05);
        let count = (duration * 120.).ceil() as usize;
        let parameters = IntegrationParameters {
            dt: duration / count as f32,
            max_ccd_substeps: 4,
            ..IntegrationParameters::default()
        };
        for _ in 0..count {
            self.pipeline.step(
                &vector![0., 0., -9.81],
                &parameters,
                &mut self.islands,
                &mut self.broad,
                &mut self.narrow,
                &mut self.bodies,
                &mut self.colliders,
                &mut self.joints,
                &mut self.multibody,
                &mut self.ccd,
                &(),
                &(),
            );
        }
        Ok(())
    }
    pub fn state(&self, id: u64) -> Result<[f32; 14]> {
        let body = &self.bodies[self.handle(id)?];
        let p = body.position();
        let q = p.rotation.quaternion();
        let v = body.linvel();
        let a = body.angvel();
        Ok([
            p.translation.x,
            p.translation.y,
            p.translation.z,
            q.i,
            q.j,
            q.k,
            q.w,
            v.x,
            v.y,
            v.z,
            a.x,
            a.y,
            a.z,
            if body.is_sleeping() { 1. } else { 0. },
        ])
    }
}

#[cfg(test)]
mod tests {
    #[test]
    fn saved_dynamic_sleep_preserves_pose_until_woken() {
        let mut w = World::default();
        let item = w.boxes(1, &transform(12000., 9000., 1.), 1., &[0.05, 0.05, 0.05, 0., 0., 0., 0., 0., 0., 1.]).unwrap();
        w.sleep(item).unwrap();
        w.velocity(item, &[0., 0., 0.], &[0., 0., 0.]).unwrap();
        assert_eq!(w.state(item).unwrap()[13], 0.);
        w.sleep(item).unwrap();
        let saved = w.state(item).unwrap();
        assert_eq!(saved[13], 1.);
        for _ in 0..120 { w.step(1. / 120.).unwrap(); }
        assert_eq!(w.state(item).unwrap(), saved);
        w.velocity(item, &[0., 0., 0.], &[0., 0., 0.]).unwrap();
        for _ in 0..12 { w.step(1. / 120.).unwrap(); }
        assert!(w.state(item).unwrap()[2] < saved[2] - 0.02);
    }
    #[test]
    fn sleep_rejects_static_and_unknown_handles_without_mutation() {
        let mut w = World::default();
        let fixed = w.boxes(0, &transform(0., 0., 0.), 1., &[1., 1., 0.1, 0., 0., 0., 0., 0., 0., 1.]).unwrap();
        let before = w.state(fixed).unwrap();
        assert!(w.sleep(fixed).is_err());
        assert!(w.sleep(u64::MAX).is_err());
        assert_eq!(w.state(fixed).unwrap(), before);
    }
    use super::*;
    #[test]
    fn zomboid_map_positions_and_queries_are_supported() {
        let mut world = World::default();
        let wall = world
            .boxes(
                0,
                &transform(12_001.5, 9_000., 0.5),
                1.,
                &cuboid(0.1, 1., 0.5),
            )
            .unwrap();
        let item = world
            .boxes(
                1,
                &transform(12_000., 9_000., 0.5),
                0.2,
                &cuboid(0.04, 0.04, 0.04),
            )
            .unwrap();
        assert!(world.distance(wall, &[12_000., 9_000., 0.5]).unwrap() > 1.);
        assert!(
            world
                .raycast(&[12_000., 9_000., 0.5, 1., 0., 0., 2.], &[item])
                .unwrap()
                .is_some()
        );
        assert!(
            world
                .capsule_sweep(&[12_000., 9_000., 0., 1., 0., 0., 0.1, 1., 2.], &[item])
                .unwrap()
                .is_some()
        );
        assert!(
            world
                .boxes(1, &transform(100_001., 0., 0.), 1., &cuboid(0.1, 0.1, 0.1))
                .is_err()
        );
        assert_eq!(
            world.ids.len(),
            2,
            "bad far coordinate must not partially allocate a body"
        );
    }
    fn transform(x: f32, y: f32, z: f32) -> [f32; 7] {
        [x, y, z, 0., 0., 0., 1.]
    }
    fn cuboid(x: f32, y: f32, z: f32) -> [f32; 10] {
        [x, y, z, 0., 0., 0., 0., 0., 0., 1.]
    }
    fn run(world: &mut World, count: usize) {
        for _ in 0..count {
            world.step(1. / 120.).unwrap();
        }
    }
    #[test]
    fn counterbalance_preserves_dynamic_collision_and_restores_gravity() {
        let mut world = World::default();
        let body = world
            .boxes(1, &transform(0., 0., 1.), 4., &cuboid(0.2, 0.03, 0.2))
            .unwrap();
        world.gravity_scale(body, 0.).unwrap();
        run(&mut world, 120);
        assert!((world.state(body).unwrap()[2] - 1.).abs() < 0.001);
        world.velocity(body, &[0.1, 0., 0.], &[0.; 3]).unwrap();
        run(&mut world, 120);
        assert!(
            world.state(body).unwrap()[0] > 0.05,
            "balanced body stays dynamic"
        );
        world.gravity_scale(body, 1.).unwrap();
        run(&mut world, 120);
        assert!(
            world.state(body).unwrap()[2] < 0.5,
            "gravity can be restored"
        );
    }
    #[test]
    fn invalid_counterbalance_is_atomic() {
        let mut world = World::default();
        let body = world
            .boxes(1, &transform(0., 0., 1.), 4., &cuboid(0.2, 0.03, 0.2))
            .unwrap();
        let fixed = world
            .boxes(0, &transform(1., 0., 1.), 1., &cuboid(0.2, 0.03, 0.2))
            .unwrap();
        for value in [f32::NAN, f32::INFINITY, -0.1, 1.1] {
            assert!(world.gravity_scale(body, value).is_err());
        }
        assert!(world.gravity_scale(fixed, 0.).is_err());
        assert!(world.gravity_scale(u64::MAX, 0.).is_err());
        run(&mut world, 120);
        assert!(
            world.state(body).unwrap()[2] < 0.5,
            "rejection retains original gravity"
        );
    }
    #[test]
    fn triangle_floor_and_stack() {
        let mut w = World::default();
        w.mesh(
            0,
            &transform(0., 0., 0.),
            1.,
            &[-1., -1., 0., 1., -1., 0., 1., 1., 0., -1., 1., 0.],
            &[0, 1, 2, 0, 2, 3],
        )
        .unwrap();
        let mut ids = Vec::new();
        for i in 0..6 {
            ids.push(
                w.boxes(
                    1,
                    &transform(0., 0., 0.12 + i as f32 * 0.11),
                    0.1,
                    &cuboid(0.05, 0.05, 0.05),
                )
                .unwrap(),
            );
        }
        run(&mut w, 1200);
        for (i, id) in ids.iter().enumerate() {
            let state = w.state(*id).unwrap();
            assert!(
                (state[2] - (0.05 + i as f32 * 0.10)).abs() < 0.02,
                "stack body {i}: {:?}",
                state
            );
        }
    }
    #[test]
    fn ccd_thin_wall() {
        let mut w = World::default();
        w.boxes(0, &transform(0.2, 0., 0.), 1., &cuboid(0.005, 1., 1.))
            .unwrap();
        let id = w
            .boxes(1, &transform(0., 0., 0.), 0.1, &cuboid(0.01, 0.01, 0.01))
            .unwrap();
        w.velocity(id, &[20., 0., 0.], &[0., 0., 0.]).unwrap();
        w.step(0.05).unwrap();
        assert!(
            w.state(id).unwrap()[0] < 0.205,
            "CCD prevents crossing a one-centimeter wall"
        );
    }
    #[test]
    fn rotating_contacts_and_sleep() {
        let mut w = World::default();
        w.boxes(0, &transform(0., 0., -0.1), 1., &cuboid(1., 1., 0.1))
            .unwrap();
        let id = w
            .boxes(1, &transform(0., 0., 0.4), 0.2, &cuboid(0.08, 0.04, 0.03))
            .unwrap();
        w.velocity(id, &[0., 0., 0.], &[2., 1., 0.]).unwrap();
        run(&mut w, 30);
        assert!(w.state(id).unwrap()[3..6].iter().any(|x| x.abs() > 0.01));
        run(&mut w, 1600);
        assert!(w.state(id).unwrap()[2] > 0.015);
        assert_eq!(w.state(id).unwrap()[13], 1.);
    }
    #[test]
    fn motor_hinge_respects_obstacle() {
        let mut w = World::default();
        let base = w
            .boxes(0, &transform(0., 0., 0.4), 1., &cuboid(0.01, 0.01, 0.01))
            .unwrap();
        let lid = w
            .boxes(1, &transform(-0.2, 0., 0.4), 0.2, &cuboid(0.2, 0.01, 0.1))
            .unwrap();
        let joint = w
            .hinge(
                base,
                lid,
                &[0., 0., 0.],
                &[0.2, 0., 0.],
                &[0., 0., 1.],
                0.,
                1.5,
            )
            .unwrap();
        w.boxes(
            0,
            &transform(-0.18, -0.15, 0.4),
            1.,
            &cuboid(0.04, 0.04, 0.15),
        )
        .unwrap();
        w.motor(joint, 1.3, 30., 3., 0.5).unwrap();
        run(&mut w, 1000);
        let angle = w.angle(joint).unwrap();
        assert!(
            (-0.03..0.9).contains(&angle),
            "cover stops against contents: {angle}"
        );
        assert!(w.motor(joint, 1.6, 30., 3., 0.5).is_err());
    }
    #[test]
    fn fixed_storage_and_teardown() {
        let mut w = World::default();
        let bag = w
            .boxes(2, &transform(0., 0., 0.), 1., &cuboid(0.01, 0.01, 0.01))
            .unwrap();
        let item = w
            .boxes(1, &transform(0.1, 0., 0.), 0.1, &cuboid(0.01, 0.01, 0.01))
            .unwrap();
        w.pin(bag, item, &transform(0.1, 0., 0.)).unwrap();
        w.drive(bag, &transform(0., 0., 0.5)).unwrap();
        run(&mut w, 120);
        assert!((w.state(item).unwrap()[2] - 0.5).abs() < 0.01);
        w.remove(item).unwrap();
        assert!(w.state(item).is_err());
        assert!(w.joint_ids.is_empty());
        assert_eq!(w.colliders.len(), 1);
    }
    #[test]
    fn slider_locks_rotation_and_cross_axes_and_obeys_limits() {
        let mut w = World::default();
        let parent = w
            .boxes(0, &transform(0., 0., 0.5), 1., &cuboid(0.01, 0.01, 0.01))
            .unwrap();
        let drawer = w
            .boxes(1, &transform(0., 0., 0.5), 1., &cuboid(0.2, 0.1, 0.05))
            .unwrap();
        let joint = w
            .slider(parent, drawer, &[0.; 3], &[0.; 3], &[0., 1., 0.], 0., 0.1)
            .unwrap();
        w.velocity(drawer, &[4., 0., 3.], &[2., 3., 4.]).unwrap();
        w.slide_motor(joint, 0.09, 300., 25., 20.).unwrap();
        run(&mut w, 480);
        let state = w.state(drawer).unwrap();
        assert!(state[0].abs() < 0.002 && (state[2] - 0.5).abs() < 0.002);
        assert!(state[3..6].iter().all(|v| v.abs() < 0.002));
        assert!((w.travel(joint).unwrap() - 0.09).abs() < 0.003);
        assert!(w.slide_motor(joint, 0.11, 300., 25., 20.).is_err());
        assert!(w.motor(joint, 0., 1., 1., 1.).is_err());
        w.remove(drawer).unwrap();
        assert!(w.travel(joint).is_err());
        assert!(
            w.slider(parent, parent, &[0.; 3], &[0.; 3], &[0., 1., 0.], 0., 0.1)
                .is_err()
        );
        assert!(
            w.slider(parent, parent, &[0.; 3], &[0.; 3], &[0.; 3], 0., 0.1)
                .is_err()
        );
    }
    #[test]
    fn slider_motor_stalls_at_an_obstacle() {
        let mut w = World::default();
        let parent = w
            .boxes(0, &transform(0., 0., 0.5), 1., &cuboid(0.01, 0.01, 0.01))
            .unwrap();
        let drawer = w
            .boxes(1, &transform(0., 0., 0.5), 1., &cuboid(0.2, 0.1, 0.05))
            .unwrap();
        let joint = w
            .slider(parent, drawer, &[0.; 3], &[0.; 3], &[0., 1., 0.], 0., 0.1)
            .unwrap();
        w.boxes(0, &transform(0., 0.155, 0.5), 1., &cuboid(0.3, 0.01, 0.1))
            .unwrap();
        w.slide_motor(joint, 0.1, 300., 25., 20.).unwrap();
        run(&mut w, 480);
        assert!(w.travel(joint).unwrap() < 0.05);
        assert!(w.slide_motor(joint, f32::NAN, 1., 1., 1.).is_err());
    }
    #[test]
    fn invalid_geometry_is_atomic() {
        let mut w = World::default();
        assert!(
            w.boxes(1, &transform(0., 0., 0.), 1., &cuboid(f32::NAN, 0.1, 0.1))
                .is_err()
        );
        assert!(
            w.mesh(1, &transform(0., 0., 0.), 1., &[0.; 12], &[0, 1, 8])
                .is_err()
        );
        assert!(w.step(f32::NAN).is_err());
        assert!(w.ids.is_empty() && w.colliders.is_empty());
    }
    fn held_target(x: f32, angle: f32) -> [f32; 13] {
        [
            x,
            0.,
            0.4,
            0.,
            0.,
            (angle / 2.).sin(),
            (angle / 2.).cos(),
            700.,
            60.,
            80.,
            12.,
            1.,
            4.,
        ]
    }
    #[test]
    fn six_dof_hold_respects_contacts_and_release() {
        let mut w = World::default();
        w.boxes(0, &transform(0.3, 0., 0.4), 1., &cuboid(0.01, 0.5, 0.4))
            .unwrap();
        let item = w
            .boxes(1, &transform(0., 0., 0.4), 0.7, &cuboid(0.04, 0.03, 0.02))
            .unwrap();
        w.spring(item, &held_target(0.7, 0.)).unwrap();
        run(&mut w, 600);
        assert!(
            w.state(item).unwrap()[0] < 0.3,
            "motor must not pull a held item through the wall"
        );
        assert_eq!(
            w.bodies.len(),
            3,
            "one collision-free target owns the held drive"
        );
        w.spring(item, &held_target(-0.1, 0.7)).unwrap();
        run(&mut w, 600);
        let state = w.state(item).unwrap();
        assert!(
            (state[0] + 0.1).abs() < 0.015 && (state[2] - 0.4).abs() < 0.02,
            "released contact follows the target: {state:?}"
        );
        assert!(
            (state[5] * (0.35_f32).sin() + state[6] * (0.35_f32).cos()).abs() > 0.99,
            "rotational motor follows the physical orientation"
        );
        w.release_spring(item).unwrap();
        assert_eq!(w.bodies.len(), 2);
        w.velocity(item, &[-0.3, 0., 1.], &[0., 0., 2.]).unwrap();
        w.step(0.05).unwrap();
        assert!(
            w.state(item).unwrap()[2] > state[2],
            "native release momentum is retained"
        );
        w.remove(item).unwrap();
        assert!(w.springs.is_empty() && w.joints.is_empty() && w.bodies.len() == 1);
    }
    #[test]
    fn invalid_held_drive_is_atomic() {
        let mut w = World::default();
        let fixed = w
            .boxes(0, &transform(0., 0., 0.), 1., &cuboid(0.1, 0.1, 0.1))
            .unwrap();
        let item = w
            .boxes(1, &transform(0., 0., 0.4), 0.7, &cuboid(0.04, 0.03, 0.02))
            .unwrap();
        assert!(w.spring(fixed, &held_target(0., 0.)).is_err());
        let mut target = held_target(0., 0.);
        target[9] = 501.;
        assert!(w.spring(item, &target).is_err());
        target = held_target(0., 0.);
        target[3] = f32::NAN;
        assert!(w.spring(item, &target).is_err());
        assert_eq!(w.bodies.len(), 2);
        assert!(w.springs.is_empty() && w.joints.is_empty());
        w.spring(item, &held_target(0., 0.)).unwrap();
        w.remove(item).unwrap();
        assert!(w.springs.is_empty() && w.joints.is_empty() && w.bodies.len() == 1);
    }
    #[test]
    fn held_orientation_resists_offcentre_gravity() {
        let mut w = World::default();
        let mut geometry = cuboid(0.2, 0.05, 0.05);
        geometry[3] = 0.15;
        let item = w.boxes(1, &transform(0., 0., 0.4), 0.7, &geometry).unwrap();
        let mut target = held_target(0., 0.);
        target[7..].copy_from_slice(&[700., 49., 140., 56., 2.8, 5.6]);
        w.spring(item, &target).unwrap();
        run(&mut w, 1200);
        let state = w.state(item).unwrap();
        assert!(
            (state[2] - 0.4).abs() < 0.02,
            "held translation resists gravity"
        );
        assert!(
            state[3].abs() + state[4].abs() + state[5].abs() < 0.03,
            "offcentre held geometry retains tracked orientation: {state:?}"
        );
    }
    #[test]
    fn surface_query_uses_collider_geometry() {
        let mut w = World::default();
        let body = w
            .boxes(1, &transform(0.4, 0., 0.5), 0.7, &cuboid(0.25, 0.25, 0.25))
            .unwrap();
        assert!((w.distance(body, &[0.125, 0., 0.5]).unwrap() - 0.025).abs() < 0.001);
        assert!(w.distance(body, &[0.4, 0., 0.5]).unwrap() < 0.001);
        assert!(w.distance(body, &[f32::NAN, 0., 0.5]).is_err());
        w.remove(body).unwrap();
        assert!(w.distance(body, &[0., 0., 0.]).is_err());
    }
    #[test]
    fn bounded_ray_returns_closest_surface_and_respects_held_exclusions() {
        let mut w = World::default();
        let near = w
            .boxes(0, &transform(0., 0., 1.), 1., &cuboid(0.2, 0.2, 0.1))
            .unwrap();
        w.boxes(0, &transform(0., 0., 0.), 1., &cuboid(1., 1., 0.1))
            .unwrap();
        let ray = [0., 0., 2., 0., 0., -1., 3.];
        let first = w.raycast(&ray, &[]).unwrap().unwrap();
        assert!((first[3] - 1.1).abs() < 0.001 && first[6] > 0.99);
        let floor = w.raycast(&ray, &[near]).unwrap().unwrap();
        assert!((floor[3] - 0.1).abs() < 0.001);
        assert!(
            w.raycast(&[2., 2., 2., 0., 0., -1., 3.], &[])
                .unwrap()
                .is_none()
        );
        assert!(w.raycast(&[0., 0., 2., 0., 0., 0., 3.], &[]).is_err());
        assert!(
            w.raycast(&[0., 0., 2., 0., 0., -1., f32::NAN], &[])
                .is_err()
        );
        assert!(w.raycast(&ray, &[0; 257]).is_err());
    }
    #[test]
    fn scaled_static_meshes_do_not_expand_the_dynamic_vertex_budget() {
        let mut w = World::default();
        let points = [0., 0., 0., 1., 0., 0., 0., 1., 0., 0., 0., 9.8];
        let faces = [0, 1, 2, 0, 2, 3, 0, 3, 1, 1, 3, 2];
        assert!(
            w.mesh(0, &transform(0., 0., 0.), 1., &points, &faces)
                .is_ok()
        );
        assert!(
            w.mesh(1, &transform(0., 0., 0.), 1., &points, &faces)
                .is_err()
        );
        let large = [0., 0., 0., 17., 0., 0., 0., 1., 0., 0., 0., 1.];
        assert!(
            w.mesh(0, &transform(0., 0., 0.), 1., &large, &faces)
                .is_err()
        );
    }
    #[test]
    fn capsule_cast_stops_at_walls_and_ignores_own_equipment() {
        let mut w = World::default();
        let wall = w
            .boxes(
                0,
                &transform(0.8, 0., 0.8),
                1.,
                &[0.05, 2., 0.8, 0., 0., 0., 0., 0., 0., 1.],
            )
            .unwrap();
        let query = [0., 0., 0.15, 1., 0., 0., 0.19, 1.5, 1.];
        let hit = w.capsule_sweep(&query, &[]).unwrap().unwrap();
        assert!((hit[0] - 0.56).abs() < 0.005 && hit[1] < -0.99);
        assert!(w.capsule_sweep(&query, &[wall]).unwrap().is_none());
        assert!(
            w.capsule_sweep(&[0., 0., 0.15, -1., 0., 0., 0.19, 1.5, 1.], &[])
                .unwrap()
                .is_none()
        );
    }
    #[test]
    fn capsule_cast_allows_escape_from_initial_overlap() {
        let mut w = World::default();
        w.boxes(
            0,
            &transform(0.2, 0., 0.8),
            1.,
            &[0.1, 2., 0.8, 0., 0., 0., 0., 0., 0., 1.],
        )
        .unwrap();
        assert!(
            w.capsule_sweep(&[0., 0., 0.15, -1., 0., 0., 0.19, 1.5, 0.4], &[])
                .unwrap()
                .is_none()
        );
        let hit = w
            .capsule_sweep(&[0., 0., 0.15, 1., 0., 0., 0.19, 1.5, 0.4], &[])
            .unwrap()
            .unwrap();
        assert!(hit[0] < 0.001);
    }
    #[test]
    fn capsule_cast_preserves_low_clearance_and_foot_space() {
        let mut w = World::default();
        w.boxes(
            0,
            &transform(0., 0., -0.01),
            1.,
            &[2., 2., 0.01, 0., 0., 0., 0., 0., 0., 1.],
        )
        .unwrap();
        w.boxes(
            0,
            &transform(0., 0., 1.3),
            1.,
            &[2., 2., 0.05, 0., 0., 0., 0., 0., 0., 1.],
        )
        .unwrap();
        assert!(
            w.capsule_sweep(&[0., 0., 0.15, 1., 0., 0., 0.19, 0.9, 0.3], &[])
                .unwrap()
                .is_none()
        );
        assert!(
            w.capsule_sweep(&[0., 0., 0.15, 0., 0., 1., 0.19, 0.9, 0.5], &[])
                .unwrap()
                .is_some()
        );
    }
    #[test]
    fn capsule_cast_validates_numeric_and_array_budgets() {
        let w = World::default();
        let valid = [0., 0., 0.15, 1., 0., 0., 0.19, 1.5, 0.3];
        assert!(w.capsule_sweep(&valid, &[]).unwrap().is_none());
        for (index, value) in [(0, f32::NAN), (3, 0.), (6, 0.), (7, 0.1), (8, 3.)] {
            let mut bad = valid;
            bad[index] = value;
            assert!(w.capsule_sweep(&bad, &[]).is_err());
        }
        assert!(w.capsule_sweep(&valid, &[0; 257]).is_err());
        assert!(w.capsule_sweep(&valid[..8], &[]).is_err());
    }
}
