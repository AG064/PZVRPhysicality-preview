use super::*;
use jni::{
    JNIEnv,
    objects::{JClass, JFloatArray, JIntArray},
    sys::{jint, jlong},
};
use std::sync::{Mutex, OnceLock};

#[derive(Default)]
struct Registry {
    next: u64,
    worlds: BTreeMap<u64, World>,
}
static WORLDS: OnceLock<Mutex<Registry>> = OnceLock::new();

#[allow(clippy::too_many_arguments)] // JNI arrays and handles stay explicit at the boundary.
fn execute(
    env: &mut JNIEnv,
    world: jlong,
    op: jint,
    a: jlong,
    b: jlong,
    input: &JFloatArray,
    faces: &JIntArray,
    output: &JFloatArray,
) -> std::result::Result<jlong, String> {
    let count = env.get_array_length(input).map_err(|e| e.to_string())? as usize;
    let face_count = env.get_array_length(faces).map_err(|e| e.to_string())? as usize;
    let out_count = env.get_array_length(output).map_err(|e| e.to_string())? as usize;
    if count > 96_008 || face_count > 192_000 || out_count > 14 {
        return Err("JNI array budget".into());
    }
    let mut values = vec![0.; count];
    let mut indices = vec![0; face_count];
    env.get_float_array_region(input, 0, &mut values)
        .map_err(|e| e.to_string())?;
    env.get_int_array_region(faces, 0, &mut indices)
        .map_err(|e| e.to_string())?;
    if out_count
        != match op {
            10 | 16 | 19 | 23 => 1,
            12 => 14,
            20 => 7,
            21 => 4,
            _ => 0,
        }
    {
        return Err("JNI output shape".into());
    }
    if op != 3 && op != 15 && op != 20 && op != 21 && op != 23 && op != 24 && !indices.is_empty() {
        return Err("Unexpected face input".into());
    }
    let mut registry = WORLDS
        .get_or_init(|| Mutex::new(Registry::default()))
        .lock()
        .map_err(|_| "Physics registry unavailable")?;
    if op == 0 {
        if world != 0 || a != 0 || b != 0 || count != 0 || registry.worlds.len() >= 8 {
            return Err("World creation budget or inputs".into());
        }
        registry.next = registry
            .next
            .checked_add(1)
            .ok_or("World handle overflow")?;
        let id = registry.next;
        registry.worlds.insert(id, World::default());
        return Ok(id as jlong);
    }
    let world_id = u64::try_from(world).map_err(|_| "World handle")?;
    if op == 1 {
        if count != 0 || a != 0 || b != 0 || registry.worlds.remove(&world_id).is_none() {
            return Err("Unknown world or close inputs".into());
        }
        return Ok(1);
    }
    let world = registry
        .worlds
        .get_mut(&world_id)
        .ok_or("Unknown world handle")?;
    let aid = u64::try_from(a).map_err(|_| "Body handle")?;
    let bid = u64::try_from(b).map_err(|_| "Body handle")?;
    let result: Result<u64> = match op {
        24 if count >= 20 && a == 0 && b == 0 => world
            .prepared_mesh(&values[1..8], values[0], &values[8..], &indices)
            .map(|id| id.unwrap_or(0)),
        23 if count >= 12 && a == 0 && b == 0 => {
            let state = world.prepare_mesh(&values, &indices)?;
            env.set_float_array_region(output, 0, &[state as f32])
                .map_err(|e| e.to_string())?;
            Ok(1)
        }
        2 if count >= 18 && (0..=2).contains(&a) && b == 0 => {
            world.boxes(a as i32, &values[1..8], values[0], &values[8..])
        }
        3 if count >= 20 && (0..=2).contains(&a) && b == 0 => {
            world.mesh(a as i32, &values[1..8], values[0], &values[8..], &indices)
        }
        4 if count == 0 && b == 0 => world.remove(aid).map(|_| 1),
        5 if count == 7 && b == 0 => world.drive(aid, &values).map(|_| 1),
        6 if count == 6 && b == 0 => world.velocity(aid, &values[..3], &values[3..]).map(|_| 1),
        25 if count == 0 && b == 0 => world.sleep(aid).map(|_| 1),
        7 if count == 7 => world.pin(aid, bid, &values),
        8 if count == 11 => world.hinge(
            aid,
            bid,
            &values[..3],
            &values[3..6],
            &values[6..9],
            values[9],
            values[10],
        ),
        9 if count == 4 && b == 0 => world
            .motor(aid, values[0], values[1], values[2], values[3])
            .map(|_| 1),
        10 if count == 0 && b == 0 => {
            let angle = world.angle(aid)?;
            env.set_float_array_region(output, 0, &[angle])
                .map_err(|e| e.to_string())?;
            Ok(1)
        }
        11 if count == 1 && a == 0 && b == 0 => world.step(values[0]).map(|_| 1),
        12 if count == 0 && b == 0 => {
            let state = world.state(aid)?;
            env.set_float_array_region(output, 0, &state)
                .map_err(|e| e.to_string())?;
            Ok(1)
        }
        13 if count == 13 && b == 0 => world.spring(aid, &values).map(|_| 1),
        14 if count == 0 && b == 0 => world.release_spring(aid).map(|_| 1),
        15 if count == 0 && b == 0 && face_count == 2 => world
            .groups(aid, indices[0] as u32, indices[1] as u32)
            .map(|_| 1),
        16 if count == 3 && b == 0 => {
            let distance = world.distance(aid, &values)?;
            env.set_float_array_region(output, 0, &[distance])
                .map_err(|e| e.to_string())?;
            Ok(1)
        }
        17 if count == 11 => world.slider(
            aid,
            bid,
            &values[..3],
            &values[3..6],
            &values[6..9],
            values[9],
            values[10],
        ),
        18 if count == 4 && b == 0 => world
            .slide_motor(aid, values[0], values[1], values[2], values[3])
            .map(|_| 1),
        19 if count == 0 && b == 0 => {
            let travel = world.travel(aid)?;
            env.set_float_array_region(output, 0, &[travel])
                .map_err(|e| e.to_string())?;
            Ok(1)
        }
        22 if count == 1 && b == 0 => world.gravity_scale(aid, values[0]).map(|_| 1),
        21 if count == 9
            && a == 0
            && b == 0
            && face_count <= 512
            && face_count.is_multiple_of(2) =>
        {
            let excluded: Vec<_> = indices
                .as_chunks::<2>()
                .0
                .iter()
                .map(|pair| pair[0] as u32 as u64 | ((pair[1] as u32 as u64) << 32))
                .collect();
            match world.capsule_sweep(&values, &excluded)? {
                Some(hit) => {
                    env.set_float_array_region(output, 0, &hit)
                        .map_err(|e| e.to_string())?;
                    Ok(1)
                }
                None => Ok(0),
            }
        }
        20 if count == 7
            && a == 0
            && b == 0
            && face_count <= 512
            && face_count.is_multiple_of(2) =>
        {
            let excluded: Vec<u64> = indices
                .as_chunks::<2>()
                .0
                .iter()
                .map(|pair| pair[0] as u32 as u64 | ((pair[1] as u32 as u64) << 32))
                .collect();
            match world.raycast(&values, &excluded)? {
                Some(hit) => {
                    env.set_float_array_region(output, 0, &hit)
                        .map_err(|e| e.to_string())?;
                    Ok(1)
                }
                None => Ok(0),
            }
        }
        _ => Err("Unsupported JNI command or input size"),
    };
    result.map(|id| id as jlong).map_err(str::to_owned)
}

/// All references are JNI-owned arrays, never external pointers or game object addresses.
#[unsafe(no_mangle)]
pub extern "system" fn Java_viewpointvr_physics_RigidPhysics_nCall(
    mut env: JNIEnv,
    _: JClass,
    world: jlong,
    op: jint,
    a: jlong,
    b: jlong,
    input: JFloatArray,
    faces: JIntArray,
    output: JFloatArray,
) -> jlong {
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        execute(&mut env, world, op, a, b, &input, &faces, &output)
    }));
    match result {
        Ok(Ok(value)) => value,
        other => {
            let message = match other {
                Ok(Err(error)) => error,
                _ => "Physics operation failed; registry disabled".into(),
            };
            if !env.exception_check().unwrap_or(true) {
                let _ = env.throw_new("java/lang/IllegalStateException", message);
            }
            0
        }
    }
}
