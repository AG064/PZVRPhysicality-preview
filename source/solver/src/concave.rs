//! Detached, bounded collider preparation. No physics world or JNI buffer enters the worker.
use super::*;
use rapier3d::parry::transformation::{vhacd::VHACDParameters, voxelization::FillMode};
use std::collections::VecDeque;
use std::sync::{Arc, Mutex, OnceLock, mpsc};

const CACHE_LIMIT: usize = 32;
const RAW_LIMIT: usize = 16 * 1024 * 1024;
const PENDING_LIMIT: usize = 8;
#[derive(Clone, PartialEq)]
struct Input {
    coordinates: Vec<i32>,
    faces: Vec<[u32; 3]>,
}
impl Input {
    fn read(points: &[f32], indices: &[i32]) -> Result<Self> {
        if points.len() < 12
            || !points.len().is_multiple_of(3)
            || points.len() > 96_000
            || indices.len() < 3
            || !indices.len().is_multiple_of(3)
            || indices.len() > 192_000
        {
            return Err("Dynamic geometry extent");
        }
        finite(points, 4.)?;
        if indices
            .iter()
            .any(|i| *i < 0 || *i as usize >= points.len() / 3)
        {
            return Err("Dynamic triangle outside mesh");
        }
        // Local 0.1 mm quantization keeps harmless camera-subtraction roundoff from rebuilding colliders.
        Ok(Self {
            coordinates: points
                .iter()
                .map(|v| (v * 10_000.).round() as i32)
                .collect(),
            faces: indices
                .as_chunks::<3>()
                .0
                .iter()
                .map(|v| [v[0] as u32, v[1] as u32, v[2] as u32])
                .collect(),
        })
    }
    fn bytes(&self) -> usize {
        self.coordinates.len() * 4 + self.faces.len() * 12
    }
    fn vertices(&self) -> Vec<Point<f32>> {
        self.coordinates
            .as_chunks::<3>()
            .0
            .iter()
            .map(|v| {
                point![
                    v[0] as f32 / 10_000.,
                    v[1] as f32 / 10_000.,
                    v[2] as f32 / 10_000.
                ]
            })
            .collect()
    }
}
enum State {
    Pending,
    Ready(SharedShape),
    Failed,
}
#[derive(Default)]
struct Cache {
    entries: VecDeque<(Arc<Input>, State)>,
}
static CACHE: OnceLock<Mutex<Cache>> = OnceLock::new();
static WORKER: OnceLock<Result<mpsc::SyncSender<Arc<Input>>>> = OnceLock::new();
fn cache() -> &'static Mutex<Cache> {
    CACHE.get_or_init(|| Mutex::new(Cache::default()))
}
fn stored(input: &Input) -> Result<Option<SharedShape>> {
    let entries = cache().lock().map_err(|_| "Geometry cache unavailable")?;
    Ok(entries
        .entries
        .iter()
        .find(|(v, _)| v.as_ref() == input)
        .and_then(|(_, s)| match s {
            State::Ready(shape) => Some(shape.clone()),
            _ => None,
        }))
}
pub(super) fn prepared(points: &[f32], indices: &[i32]) -> Result<Option<SharedShape>> {
    stored(&Input::read(points, indices)?)
}
fn store(input: Arc<Input>, result: Result<SharedShape>) {
    if let Ok(mut c) = cache().lock()
        && let Some((_, state)) = c
            .entries
            .iter_mut()
            .find(|(v, _)| v.as_ref() == input.as_ref())
    {
        *state = match result {
            Ok(shape) => State::Ready(shape),
            Err(_) => State::Failed,
        };
    }
}
fn reserve(c: &mut Cache, input: Arc<Input>) -> bool {
    while c.entries.len() >= CACHE_LIMIT
        || c.entries.iter().map(|(v, _)| v.bytes()).sum::<usize>() + input.bytes() > RAW_LIMIT
    {
        let Some(index) = c
            .entries
            .iter()
            .position(|(_, s)| !matches!(s, State::Pending))
        else {
            return false;
        };
        c.entries.remove(index);
    }
    c.entries.push_back((input, State::Pending));
    true
}
fn worker() -> Result<&'static mpsc::SyncSender<Arc<Input>>> {
    WORKER
        .get_or_init(|| {
            let (tx, rx) = mpsc::sync_channel::<Arc<Input>>(PENDING_LIMIT);
            std::thread::Builder::new()
                .name("pzvr-collider-preparation".into())
                .spawn(move || {
                    while let Ok(input) = rx.recv() {
                        let result = compute(&input);
                        store(input, result);
                    }
                })
                .map_err(|_| "Collider worker unavailable")?;
            Ok(tx)
        })
        .as_ref()
        .map_err(|error| *error)
}
/// 0 pending/busy, 1 ready, -1 failed. Never blocks on decomposition.
pub(super) fn prepare(points: &[f32], indices: &[i32]) -> Result<i32> {
    let input = Arc::new(Input::read(points, indices)?);
    {
        let mut c = cache().lock().map_err(|_| "Geometry cache unavailable")?;
        if let Some((_, s)) = c.entries.iter().find(|(v, _)| v.as_ref() == input.as_ref()) {
            return Ok(match s {
                State::Pending => 0,
                State::Ready(_) => 1,
                State::Failed => -1,
            });
        }
        if c.entries
            .iter()
            .filter(|(_, s)| matches!(s, State::Pending))
            .count()
            >= PENDING_LIMIT
            || !reserve(&mut c, input.clone())
        {
            return Ok(0);
        }
    }
    let sender = match worker() {
        Ok(sender) => sender,
        Err(error) => {
            store(input, Err(error));
            return Ok(-1);
        }
    };
    if sender.try_send(input.clone()).is_err() {
        let mut c = cache().lock().map_err(|_| "Geometry cache unavailable")?;
        c.entries.retain(|(v, _)| v.as_ref() != input.as_ref());
    }
    Ok(0)
}
/// Synchronous callers remain supported for tools/tests; gameplay prepares before body creation.
pub(super) fn shape(points: &[f32], indices: &[i32]) -> Result<SharedShape> {
    let input = Arc::new(Input::read(points, indices)?);
    if let Some(shape) = stored(&input)? {
        return Ok(shape);
    }
    let result = compute(&input);
    {
        let mut c = cache().lock().map_err(|_| "Geometry cache unavailable")?;
        if !c.entries.iter().any(|(v, _)| v.as_ref() == input.as_ref()) {
            reserve(&mut c, input.clone());
        }
    }
    store(input, result.clone());
    result
}
fn convex_surface(vertices: &[Point<f32>], faces: &[[u32; 3]]) -> bool {
    if vertices.len() > 128 || faces.len() > 256 {
        return false;
    }
    let mut edges = BTreeMap::new();
    for face in faces {
        let a = vertices[face[0] as usize];
        let b = vertices[face[1] as usize];
        let d = vertices[face[2] as usize];
        let n = (b - a).cross(&(d - a));
        if n.norm_squared() < 1e-12 {
            return false;
        }
        let mut positive = false;
        let mut negative = false;
        let tolerance = n.norm() * 0.0001;
        for v in vertices {
            let distance = n.dot(&(v - a));
            positive |= distance > tolerance;
            negative |= distance < -tolerance;
            if positive && negative {
                return false;
            }
        }
        for (a, b) in [(face[0], face[1]), (face[1], face[2]), (face[2], face[0])] {
            *edges.entry((a.min(b), a.max(b))).or_insert(0) += 1;
        }
    }
    edges.values().all(|count| *count == 2)
}
fn compute(input: &Input) -> Result<SharedShape> {
    std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        let vertices = input.vertices();
        if convex_surface(&vertices, &input.faces) {
            return SharedShape::convex_hull(&vertices).ok_or("Degenerate convex item");
        }
        // Disconnected native model parts stay disconnected. One hull across attachments fills their gaps.
        let mut parents: Vec<usize> = (0..vertices.len()).collect();
        fn root(parents: &mut [usize], mut v: usize) -> usize {
            while parents[v] != v {
                parents[v] = parents[parents[v]];
                v = parents[v];
            }
            v
        }
        for face in &input.faces {
            let a = root(&mut parents, face[0] as usize);
            for &v in &face[1..] {
                let b = root(&mut parents, v as usize);
                parents[b] = a;
            }
        }
        let mut components = BTreeMap::<usize, Vec<[u32; 3]>>::new();
        for face in &input.faces {
            let key = root(&mut parents, face[0] as usize);
            components.entry(key).or_default().push(*face);
        }
        if (2..=64).contains(&components.len()) {
            let mut parts = Vec::new();
            let mut valid = true;
            for triangles in components.values() {
                let mut remap = BTreeMap::new();
                let mut points = Vec::new();
                let mut faces = Vec::new();
                for triangle in triangles {
                    let mut f = [0; 3];
                    for i in 0..3 {
                        f[i] = *remap.entry(triangle[i]).or_insert_with(|| {
                            points.push(vertices[triangle[i] as usize]);
                            (points.len() - 1) as u32
                        });
                    }
                    faces.push(f);
                }
                if points.len() < 4 || !convex_surface(&points, &faces) {
                    valid = false;
                    break;
                }
                if let Some(shape) = SharedShape::convex_hull(&points) {
                    parts.push((Isometry::identity(), shape));
                } else {
                    valid = false;
                    break;
                }
            }
            if valid {
                return Ok(SharedShape::compound(parts));
            }
        }
        let params = VHACDParameters {
            resolution: 96,
            concavity: 0.002,
            max_convex_hulls: 64,
            fill_mode: FillMode::FloodFill {
                detect_cavities: true,
            },
            ..VHACDParameters::default()
        };
        let shape = SharedShape::convex_decomposition_with_params(&vertices, &input.faces, &params);
        let compound = shape.as_compound().ok_or("Decomposition is not compound")?;
        if compound.shapes().is_empty()
            || compound.shapes().len() > 64
            || compound
                .shapes()
                .iter()
                .map(|(_, s)| s.as_convex_polyhedron().map_or(0, |p| p.points().len()))
                .sum::<usize>()
                > 32000
        {
            return Err("Decomposed part budget");
        }
        Ok(shape)
    }))
    .map_err(|_| "Collider decomposition failed")?
}

#[cfg(test)]
mod tests {
    use super::*;
    fn arms() -> (Vec<f32>, Vec<i32>) {
        let triangles = [
            0, 1, 3, 0, 3, 2, 4, 6, 7, 4, 7, 5, 0, 4, 5, 0, 5, 1, 2, 3, 7, 2, 7, 6, 0, 2, 6, 0, 6,
            4, 1, 5, 7, 1, 7, 3,
        ];
        let mut v = vec![];
        let mut f = vec![];
        for part in 0..2 {
            let (w, d) = if part == 0 { (0.1, 0.3) } else { (0.3, 0.1) };
            for corner in 0..8 {
                v.extend([
                    if corner & 1 == 0 { 0. } else { w },
                    if corner & 2 == 0 { 0. } else { d },
                    if corner & 4 == 0 { 0. } else { 0.2 },
                ]);
            }
            f.extend(triangles.iter().map(|i| i + part * 8));
        }
        (v, f)
    }
    #[test]
    fn dynamic_concavity_survives() {
        let (v, f) = arms();
        let mut w = World::default();
        let id = w
            .mesh(1, &[0., 0., 0., 0., 0., 0., 1.], 1., &v, &f)
            .unwrap();
        assert!(
            w.distance(id, &[0.18, 0.18, 0.1]).unwrap() > 0.025,
            "the L-shaped item's open corner must remain empty"
        );
        assert!(
            w.distance(id, &[0.05, 0.15, 0.1]).unwrap() < 0.001,
            "actual material remains solid"
        );
    }
    #[test]
    fn asynchronous_preparation_is_bounded_and_reusable() {
        let (v, f) = arms();
        let start = std::time::Instant::now();
        let mut state = prepare(&v, &f).unwrap();
        while state == 0 && start.elapsed() < std::time::Duration::from_secs(30) {
            std::thread::sleep(std::time::Duration::from_millis(10));
            state = prepare(&v, &f).unwrap();
        }
        assert_eq!(state, 1);
        assert_eq!(prepare(&v, &f).unwrap(), 1);
        assert!(prepare(&[f32::NAN; 12], &[0, 1, 2]).is_err());
    }
    #[test]
    fn connected_concave_surface_keeps_its_open_corner() {
        let polygon = [
            [0., 0.],
            [0.3, 0.],
            [0.3, 0.1],
            [0.1, 0.1],
            [0.1, 0.3],
            [0., 0.3],
        ];
        let mut vertices = vec![];
        for h in [0., 0.2] {
            for p in polygon {
                vertices.extend([p[0], p[1], h]);
            }
        }
        let mut faces = vec![];
        for t in [[0, 1, 2], [0, 2, 3], [0, 3, 5], [3, 4, 5]] {
            faces.extend([t[0], t[2], t[1]]);
            faces.extend(t.map(|v| v + 6));
        }
        for i in 0..6 {
            let j = (i + 1) % 6;
            faces.extend([i, j, j + 6, i, j + 6, i + 6]);
        }
        let mut world = World::default();
        let id = world
            .mesh(1, &[0., 0., 0., 0., 0., 0., 1.], 1., &vertices, &faces)
            .unwrap();
        assert!(
            world.distance(id, &[0.18, 0.18, 0.1]).unwrap() > 0.025,
            "connected concavity must also be preserved"
        );
    }
    #[test]
    fn hollow_cup_remains_open_and_catches_a_dropped_object() {
        let n = 12;
        let mut vertices = vec![];
        for (radius, height) in [(0.06, 0.), (0.06, 0.12), (0.045, 0.01), (0.045, 0.12)] {
            for i in 0..n {
                let a = i as f32 * std::f32::consts::TAU / n as f32;
                vertices.extend([radius * a.cos(), radius * a.sin(), height]);
            }
        }
        vertices.extend([0., 0., 0., 0., 0., 0.01]);
        let mut faces = vec![];
        for i in 0..n {
            let j = (i + 1) % n;
            faces.extend([i, j, j + n, i, j + n, i + n]);
            faces.extend([
                i + 2 * n,
                j + 3 * n,
                j + 2 * n,
                i + 2 * n,
                i + 3 * n,
                j + 3 * n,
            ]);
            faces.extend([i + n, j + n, j + 3 * n, i + n, j + 3 * n, i + 3 * n]);
            faces.extend([4 * n, j, i, 4 * n + 1, i + 2 * n, j + 2 * n]);
        }
        let mut world = World::default();
        world
            .boxes(
                0,
                &[0., 0., -0.02, 0., 0., 0., 1.],
                1.,
                &[1., 1., 0.02, 0., 0., 0., 0., 0., 0., 1.],
            )
            .unwrap();
        let cup = world
            .mesh(1, &[0., 0., 0., 0., 0., 0., 1.], 0.3, &vertices, &faces)
            .unwrap();
        assert!(
            world.distance(cup, &[0., 0., 0.08]).unwrap() > 0.025,
            "the hollow centre must not become solid"
        );
        assert!(
            world.distance(cup, &[0.055, 0., 0.08]).unwrap() < 0.005,
            "cup wall remains present"
        );
        let item = world
            .boxes(
                1,
                &[0., 0., 0.2, 0., 0., 0., 1.],
                0.01,
                &[0.008, 0.008, 0.008, 0., 0., 0., 0., 0., 0., 1.],
            )
            .unwrap();
        for _ in 0..360 {
            world.step(1. / 120.).unwrap();
        }
        let height = world.state(item).unwrap()[2] - world.state(cup).unwrap()[2];
        assert!(
            (0.005..0.05).contains(&height),
            "dropped object must settle inside the cup, height={height}"
        );
    }
}
