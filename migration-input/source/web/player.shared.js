import * as T from "three";
import { GLTFLoader } from "three/examples/jsm/loaders/GLTFLoader.js";
import { OrbitControls } from "three/examples/jsm/controls/OrbitControls.js";

const id = location.hash.slice(1);
const stage = document.querySelector("main");
function fitStage(){document.documentElement.style.height=innerHeight+"px";document.body.style.height=innerHeight+"px";stage.style.width=innerWidth+"px";stage.style.height=innerHeight+"px";}
fitStage();addEventListener("resize",fitStage);
const status = document.querySelector("output");
const menu = document.querySelector("nav");
const renderer = new T.WebGLRenderer({ antialias: true });
renderer.setPixelRatio(Math.min(devicePixelRatio, 1.5));
stage.append(renderer.domElement);
const scene = new T.Scene();
scene.background = new T.Color(0x202b39);
const camera = new T.PerspectiveCamera(42, 1, 0.01, 100);
camera.position.set(0, 1.5, 5);
const orbit = new OrbitControls(camera, renderer.domElement);
orbit.target.y = 0.15;
orbit.update();
scene.add(new T.HemisphereLight(0xfff3df, 0xadbce8, 2.2));
const key = new T.DirectionalLight(0xffffff, 2.5);
key.position.set(3, 5, 4);
scene.add(key);
const objects = new T.Group();
scene.add(objects);
const loader = new GLTFLoader();
let paused = false,
  version = 0,
  time = 0,
  focus = 50,
  simulated = true,
  tick = () => {},
  mixers = [];
window.setGameFocus = (value, demo) => {
  focus = value;
  simulated = demo;
};
const attention = () =>
  (simulated ? "模拟专注力 " : "专注力 ") + Math.round(focus);
function select(label, options, choose) {
  const element = document.createElement("select");
  element.setAttribute("aria-label", label);
  options.forEach((text, i) => {
    const o = document.createElement("option");
    o.textContent = text;
    o.value = i;
    element.append(o);
  });
  element.onchange = () => choose(+element.value);
  menu.append(element);
  return element;
}
function slider(label, min, max, value, change) {
  const wrap = document.createElement("label");
  wrap.textContent = label;
  const input = document.createElement("input");
  input.type = "range";
  input.min = min;
  input.max = max;
  input.value = value;
  input.oninput = () => change(+input.value);
  wrap.append(input);
  menu.append(wrap);
}
function action(label, callback) {
  const b = document.createElement("button");
  b.textContent = label;
  b.onclick = callback;
  menu.append(b);
}
function sound(file) {
  new Audio("/mindsoul/" + file).play().catch(() => {});
}
function dispose(node) {
  node.traverse((o) => {
    o.geometry?.dispose();
    for (const m of [].concat(o.material || [])) {
      for (const v of Object.values(m)) if (v?.isTexture) v.dispose();
      m.dispose();
    }
  });
}
function clear() {
  version++;
  mixers = [];
  for (const o of [...objects.children]) {
    objects.remove(o);
    dispose(o);
  }
}
async function model(file, x = 0, y = 0, size = 1.8) {
  const generation = version;
  const group = new T.Group();
  group.position.set(x, y, 0);
  objects.add(group);
  try {
    const gltf = await loader.loadAsync(
      file.startsWith("/") ? file : "/mindsoul/" + file,
    );
    if (generation !== version || !group.parent) {
      dispose(gltf.scene);
      return group;
    }
    const raw = gltf.scene,
      box = new T.Box3().setFromObject(raw),
      dim = box.getSize(new T.Vector3()),
      mid = box.getCenter(new T.Vector3());
    const scale = size / Math.max(dim.x, dim.y, dim.z, 0.0001);
    if (file.endsWith("native_starship.glb")) raw.traverse(o => {
      if (o.isMesh) o.material = new T.MeshStandardMaterial({color:0xc7c7c7, metalness:0.8, roughness:0.32});
    });
    raw.scale.multiplyScalar(scale);
    raw.position.sub(mid.multiplyScalar(scale));
    group.add(raw);
    group.userData.placement = {file, scale: scale * .28};
    stage.dataset.loaded = file;
    if (gltf.animations.length) {
      const mixer = new T.AnimationMixer(raw);
      mixer.clipAction(gltf.animations[0]).play();
      mixers.push(mixer);
    }
  } catch (e) {
    status.textContent = "模型加载失败：" + file.split("/").pop();
  }
  return group;
}
// Pick a scene object to drag on the camera-facing plane. Background drag orbits.
const picker = new T.Raycaster(),
  dragPlane = new T.Plane(),
  hit = new T.Vector3(),
  offset = new T.Vector3();
let dragged = null;
function ray(e) {
  const r = renderer.domElement.getBoundingClientRect();
  picker.setFromCamera(
    new T.Vector2(
      ((e.clientX - r.left) / r.width) * 2 - 1,
      1 - ((e.clientY - r.top) / r.height) * 2,
    ),
    camera,
  );
}
renderer.domElement.addEventListener(
  "pointerdown",
  (e) => {
    if (id === "blocks" || id === "brain") return;
    ray(e);
    let node = picker.intersectObjects(objects.children, true)[0]?.object;
    if (!node) return;
    while (node.parent && node.parent !== objects) node = node.parent;
    if (node.parent !== objects) return;
    dragged = node;
    orbit.enabled = false;
    dragPlane.setFromNormalAndCoplanarPoint(
      camera.getWorldDirection(new T.Vector3()),
      node.position,
    );
    picker.ray.intersectPlane(dragPlane, hit);
    offset.copy(node.position).sub(hit);
    renderer.domElement.setPointerCapture(e.pointerId);
  },
  true,
);
renderer.domElement.addEventListener("pointermove", (e) => {
  if (dragged) {
    ray(e);
    if (picker.ray.intersectPlane(dragPlane, hit))
      dragged.position.copy(hit).add(offset);
  }
});
for (const event of ["pointerup", "pointercancel"])
  renderer.domElement.addEventListener(event, () => {
    dragged = null;
    orbit.enabled = true;
  });
function start(game) {
  const files = game.assets.filter((f) => f.endsWith(".glb"));
  document.querySelector("header").textContent = game.title;
  switch (id) {
    case "cat": {
      let cats = [],
        pets = 0,
        charge = 0,
        breath = -1;
      const add = async () => {
        if (cats.length >= 6) return;
        const index = cats.length;
        cats.push(null);
        cats[index] = await model(
          files[0],
          ((index % 3) - 1) * 1.1,
          Math.floor(index / 3) * 0.7,
          1.25,
        );
      };
      add();
      action("蓄能增加猫咪", () => {
        charge = 2;
      });
      action("一起慢慢呼吸", () => {
        breath = breath < 0 ? 0 : -1;
      });
      action("随机换色", () => {
        cats.at(-1)?.traverse((o) => {
          if (o.isMesh && o.material.color)
            o.material.color.setHSL(Math.random(), 0.35, 0.8);
        });
      });
      action("抚摸猫咪", () => {
        pets++;
        status.textContent = "已抚摸 " + pets + " 次";
      });
      action("整理猫咪", () =>
        cats.forEach((c, i) => {
          if (c) c.position.x = ((i % 3) - 1) * 1.1;
        }),
      );
      tick = (dt) => {
        if (charge > 0) {
          charge -= dt;
          if (charge <= 0) add();
        }
        if (breath >= 0) {
          breath += dt;
          status.textContent = breath % 8 < 3 ? "轻柔吸气" : "缓慢呼气";
          if (breath >= 60) breath = -1;
        }
        cats.forEach((c, i) => {
          if (c) c.rotation.y = Math.sin(time * (pets ? 2 : 0.5) + i) * 0.25;
        });
      };
      break;
    }
    case "rocket": {
      let flying = false,
        h = 0,
        craft,
        heading = 0,
        tilt = 0;
      const select = async (i) => {
        clear();
        h = 0;
        flying = false;
        craft = await model(files[i]);
        if (i === 1 && craft.parent) {
          const texture = new T.TextureLoader().load("/mindsoul/NativeGames/rocket-particle2.png");
          const exhaust = new T.Group();
          for(let n=0;n<36;n++) {
            const spark = new T.Sprite(new T.SpriteMaterial({map:texture,color:n%3===0?0xffdd55:0xff8833,transparent:true,depthWrite:false,blending:T.AdditiveBlending}));
            spark.scale.set(.12,.22,1); exhaust.add(spark);
          }
          exhaust.position.y=-.9;craft.add(exhaust);craft.userData.exhaust=exhaust;
        }
      };
      select(1);
      action("无人机", () => select(0));
      action("火箭", () => select(1));
      action("开始专注力发射", () => {
        flying = true;
        sound("NativeGames/rocket-engine-synth.wav");
      });
      action("立即降落", () => (flying = false));
      action("复位", () => {
        h = 0;
        flying = false;
        heading = tilt = 0;
      });
      slider("朝向", -180, 180, 0, (v) => (heading = v));
      slider("倾角", -80, 80, 0, (v) => (tilt = v));
      tick = (dt) => {
        h = T.MathUtils.clamp(h + dt * (flying ? (focus >= 50 ? focus / 100 : -0.8) : -0.8), 0, 2);
        if (craft) {
          craft.position.y = h - 0.15;
          const exhaust=craft.userData.exhaust;
          if(exhaust){exhaust.visible=flying && focus>=50;exhaust.children.forEach((p,n)=>{
            const age=(time*1.8+n/36)%1;
            p.position.set(Math.sin(n*2.4)*age*.13,-age*.65,Math.cos(n*2.4)*age*.13);
            p.material.opacity=1-age;
          });}
          craft.rotation.set(
            0,
            (heading * Math.PI) / 180,
            (tilt * Math.PI) / 180,
          );
        }
        status.textContent = attention() + " · 高度 " + h.toFixed(1);
      };
      break;
    }
    case "race": {
      let opponents = [],
        flying = false,
        roll = 0,
        car,
        coin,
        lane = 0,
        distance = 0,
        score = 0,
        racing = false;
      model("Flight/roadster_blue.glb", 0, -0.5, 1.4).then((x) => (car = x));
      model(files[0], 0, 0, 0.4).then((x) => (coin = x));
      const grid = new T.GridHelper(20, 20);
      grid.position.y = -0.85;
      objects.add(grid);
      action("添加赛车", async () => {
        if (racing || opponents.length >= 3) return;
        const i = opponents.length;
        opponents.push(
          await model(
            [
              "Flight/roadster_red.glb",
              "Flight/roadster_green.glb",
              "Flight/roadster_white.glb",
            ][i],
            i - 1,
            -0.5,
            1,
          ),
        );
      });
      action("起飞 / 降落", () => (flying = !flying));
      action("空中翻滚", () => {
        if (flying) roll = 2 * Math.PI;
      });
      action("开始竞速", () => {
        distance = score = 0;
        racing = true;
      });
      action("左转", () => (lane = Math.max(-1, lane - 1)));
      action("右转", () => (lane = Math.min(1, lane + 1)));
      tick = (dt) => {
        if (!racing || !coin || !car) return;
        distance += dt * (3 + focus * 0.08);
        car.position.x = lane;
        car.position.y = flying ? 0.45 : -0.5;
        roll = Math.max(0, roll - dt * 5);
        car.rotation.z = roll;
        opponents.forEach(
          (o, i) => (o.position.z = -2 + Math.sin(distance * 0.05 + i)),
        );
        grid.position.z = distance % 1;
        coin.position.z = -6 + (distance % 7);
        coin.rotation.y += dt * 3;
        if (coin.position.z > 0.4) {
          if (Math.abs(coin.position.x - lane) < 0.4) {
            score++;
            sound("Flight/flight_coin_pickup.wav");
          }
          distance += 7 - (distance % 7);
          coin.position.x = Math.floor(Math.random() * 3) - 1;
        }
        if (distance >= 200) racing = false;
        status.textContent =
          (racing ? "赛程 " : "完赛 ") +
          Math.floor(distance) +
          " m · 金币 " +
          score;
      };
      break;
    }
    case "blocks":
      blocks();
      break;
    case "creation": {
      const choices = [
        ["教室", 0],
        ["花园", 1],
        ["客厅", 2],
        ["唱片机", 3],
      ];
      for (const [name, i] of choices)
        action(name, () => {
          clear();
          model(files[i]);
          status.textContent = "创作样例 · " + name;
        });
      model(files[1]);
      let track;
      const moods = [
        "calm",
        "happy",
        "focused",
        "tense",
        "low",
        "anger",
        "disgust",
        "excitement",
        "tired",
      ];
      select(
        "情绪唱片",
        [
          "平静",
          "愉快",
          "专注",
          "紧张",
          "低落",
          "愤怒",
          "厌恶",
          "兴奋",
          "疲惫",
        ],
        (i) => {
          track?.pause();
          track = new Audio(
            "/mindsoul/CreationSamples/mood-" + moods[i] + ".wav",
          );
          track.play().catch(() => {});
          clear();
          model(files[3]);
          model(files[4 + i], 0, 0.4, 1);
        },
      );
      action("花园环绕视频", () => {
        const video = document.createElement("video");
        video.controls = true;
        video.src = "/mindsoul/CreationSamples/gs-garden-orbit.mp4";
        video.style =
          "position:absolute;inset:10%;width:80%;height:70%;background:black";
        stage.append(video);
        video.play().catch(() => {});
        const close = document.createElement("button");
        close.textContent = "关闭视频";
        close.style = "position:absolute;top:10%;right:10%";
        stage.append(close);
        close.onclick = () => {
          video.pause();
          video.remove();
          close.remove();
        };
      });
      break;
    }
    case "weather": {
      let sky,
        kind = 0,
        intensity = 0.5;
      async function change(i) {
        clear();
        kind = i;
        sky = await model(files[i]);
        scene.background.setHex([0x39435f, 0x617587, 0x374052, 0x526e72][i]);
      }
      select(
        "天气",
        ["晴天", "阴天", "雨天", "雪天", "风", "乌云", "雷暴", "龙卷风"],
        (i) => {
          change([3, 1, 1, 1, 3, 1, 0, 2][i]);
          rain.visible = i === 2 || i === 3;
          rain.material.color.setHex(i === 3 ? 0xffffff : 0x89cffa);
        },
      );
      slider("强度", 0, 100, 50, (v) => (intensity = v / 100));
      const points = new Float32Array(600);
      for (let i = 0; i < points.length; i++) points[i] = Math.random() * 4 - 2;
      const geom = new T.BufferGeometry();
      geom.setAttribute("position", new T.BufferAttribute(points, 3));
      const rain = new T.Points(geom, new T.PointsMaterial({ size: 0.023 }));
      scene.add(rain);
      rain.visible = false;
      change(1);
      tick = (dt) => {
        if (rain.visible) {
          for (let j = 1; j < points.length; j += 3) {
            points[j] -= dt * (0.5 + intensity);
            if (points[j] < -2) points[j] = 2;
          }
          geom.attributes.position.needsUpdate = true;
        }
        if (sky)
          sky.rotation.y += dt * (kind === 2 ? 2 : 0.2) * (0.1 + intensity);
      };
      break;
    }
    case "popcorn": {
      let phase = 0,
        elapsed = 0,
        recipe = 0,
        recipes = [];
      const ready = fetch("/mindsoul/Popcorn/popcorn-recipes.json")
        .then((r) => r.json())
        .then((rows) => {
          recipes = rows;
          select(
            "食谱",
            rows.map((r) => r.raw),
            (i) => {
              if (phase !== 2) {
                recipe = i;
                phase = 1;
                show();
              }
            },
          );
        });
      function show() {
        clear();
        model("Popcorn/pop-fryer.glb", 0, -0.45, 1.55);
        const food = recipes[recipe];
        if (phase > 0 && food)
          model(
            "Popcorn/pop-" + (food.model || "corn") + ".glb",
            phase === 2 ? 0 : 1,
            0.3,
            0.55,
          );
      }
      action("取料", async () => {
        await ready;
        if (phase === 2) return;
        phase = 1;
        show();
        status.textContent = "已取料 · " + recipes[recipe].raw;
      });
      action("下锅", () => {
        if (phase === 1) {
          phase = 2;
          elapsed = 0;
          show();
        }
      });
      action("捞起", () => {
        if (phase === 2) {
          phase = 3;
          show();
          status.textContent =
            elapsed < recipes[recipe].seconds
              ? "尚未熟透"
              : elapsed > recipes[recipe].seconds + 3
                ? "已焦糊"
                : recipes[recipe].cooked;
        }
      });
      action("出餐", () => {
        if (phase === 3) {
          phase = 0;
          show();
          status.textContent = "已出餐";
        }
      });
      action("清理", () => {
        phase = 0;
        show();
        status.textContent = "锅已清空";
      });
      show();
      tick = (dt) => {
        if (phase === 2) {
          elapsed += dt;
          status.textContent =
            elapsed < recipes[recipe].seconds
              ? "烹饪 " + elapsed.toFixed(1) + " 秒"
              : elapsed <= recipes[recipe].seconds + 3
                ? "熟了，请捞起"
                : "已焦糊";
        }
      };
      break;
    }
    case "aquarium": {
      let animals = [],
        held = 0,
        armed = true;
      async function add(i) {
        if (animals.length >= 8) return;
        animals.push(await model(files[i], 0, 0, i === 5 ? 1.4 : 0.7));
        sound(
          i === 5
            ? "Aquarium/ocean-voice-whale.wav"
            : "Aquarium/ocean-voice-color.wav",
        );
      }
      action("添加水母", () => add(1 + Math.floor(Math.random() * 4)));
      action("添加鲸鱼", () => add(5));
      action("移除最近生物", () => {
        const a = animals.pop();
        if (a) {
          objects.remove(a);
          dispose(a);
        }
      });
      add(1);
      tick = (dt) => {
        if (focus < 50) armed = true;
        held = focus >= 70 ? held + dt * (1 + (focus - 70) / 30) : 0;
        if (armed && held >= 3) {
          armed = false;
          add(1);
        }
        animals.forEach((a, i) => {
          a.position.set(
            Math.sin(time * 0.35 + i) * 1.5,
            Math.cos(time * 0.6 + i) * 0.55,
            Math.sin(i) * 0.5,
          );
          a.rotation.y = Math.cos(time * 0.35 + i);
        });
      };
      break;
    }
    case "dragon": {
      let skin = 1,
        charge = 0,
        charging = false,
        summoned = false;
      const begin = () => {
        clear();
        model(files[0]);
        charge = 0;
        charging = true;
        summoned = false;
        sound("WishDragon/wish-charge.mp3");
      };
      model(files[0]);
      status.textContent = "保持专注力 ≥70% 达到 10 秒 · 非商用体验";
      action("MindSoul 版", () => {
        skin = 1;
        begin();
      });
      action("绿色神龙", () => {
        skin = 3;
        begin();
      });
      action("开始许愿", begin);
      tick = (dt) => {
        if (!charging) return;
        charge = focus >= 70 ? Math.min(100, charge + dt * 10) : 0;
        status.textContent =
          attention() + " · 蓄力 " + Math.floor(charge) + "%";
        if (charge >= 100 && !summoned) {
          clear();
          model(files[skin]);
          sound("WishDragon/wish-summon.mp3");
          summoned = true;
          charging = false;
          status.textContent = "神龙已召唤";
        }
      };
      break;
    }
  }
}
// Port of SpatialBlocksEngine.swift. Each level is a 5 x 5 plane.
function blocks() {
  const shapes = [
    [
      [0, 0, 0],
      [-1, 0, 0],
    ],
    [
      [0, 0, 0],
      [1, 0, 0],
      [0, 0, 1],
      [1, 0, 1],
    ],
    [
      [0, 0, 0],
      [-1, 0, 0],
      [1, 0, 0],
      [1, 0, 1],
    ],
    [
      [0, 0, 0],
      [-1, 0, 0],
      [1, 0, 1],
      [0, 0, 1],
    ],
    [
      [0, 0, 0],
      [-1, 0, 0],
      [1, 0, 0],
      [0, 0, 1],
    ],
    [[0, 0, 0]],
  ];
  const cube = new T.BoxGeometry(0.185, 0.185, 0.185);
  const paints = [
    0x88cdef, 0xf5bb77, 0x88e0ae, 0xc89bec, 0xe38e9c, 0xe4d484,
  ].map((color) => new T.MeshStandardMaterial({ color }));
  const base = new T.GridHelper(1, 5);
  base.position.y = -1.5;
  scene.add(base);
  const cage = new T.LineSegments(
    new T.EdgesGeometry(new T.BoxGeometry(1, 3, 1)),
    new T.LineBasicMaterial({ color: 0x597185 }),
  );
  scene.add(cage);
  let occupied,
    piece,
    origin,
    color,
    score,
    over,
    elapsed = 0;
  const world = (shape, at) => shape.map((c) => c.map((v, i) => v + at[i]));
  function valid(shape, at) {
    return world(shape, at).every(
      ([x, y, z]) =>
        x >= 0 &&
        x < 5 &&
        y >= 0 &&
        y < 15 &&
        z >= 0 &&
        z < 5 &&
        !occupied.has([x, y, z].join()),
    );
  }
  function spawn(i = Math.floor(Math.random() * 6)) {
    color = i;
    piece = shapes[i];
    origin = [2, 14, 2];
    over = !valid(piece, origin);
  }
  function move(dx, dy, dz) {
    const next = [origin[0] + dx, origin[1] + dy, origin[2] + dz];
    if (over || !valid(piece, next)) return false;
    origin = next;
    return true;
  }
  function lock() {
    if (over) return;
    for (const c of world(piece, origin)) occupied.set(c.join(), color);
    const levels = Array(15).fill(0);
    for (const k of occupied.keys()) levels[Number(k.split(",")[1])]++;
    const full = levels.flatMap((count, y) => (count === 25 ? [y] : []));
    if (full.length) {
      const next = new Map();
      for (const [k, v] of occupied) {
        const [x, y, z] = k.split(",").map(Number);
        if (!full.includes(y))
          next.set([x, y - full.filter((l) => l < y).length, z].join(), v);
      }
      occupied = next;
      score += 10 * full.length ** 2;
      sound("NativeGames/blocks_clear.wav");
    } else sound("NativeGames/blocks_land.wav");
    spawn();
  }
  function draw() {
    objects.clear();
    function put([x, y, z], col) {
      const o = new T.Mesh(cube, paints[col]);
      o.position.set((x - 2) * 0.2, (y - 7) * 0.2, (z - 2) * 0.2);
      objects.add(o);
    }
    for (const [k, v] of occupied) put(k.split(",").map(Number), v);
    for (const c of world(piece, origin)) put(c, color);
    status.textContent = (over ? "游戏结束 · " : "") + "得分 " + score;
  }
  function reset(demo) {
    occupied = new Map();
    score = 0;
    elapsed = 0;
    if (demo) {
      for (let x = 0; x < 5; x++)
        for (let z = 0; z < 5; z++)
          if (x !== 2 || z !== 2) occupied.set([x, 0, z].join(), (x + z) % 6);
    }
    spawn(demo ? 5 : undefined);
    draw();
  }
  for (const [label, dx, dz] of [
    ["左移", -1, 0],
    ["右移", 1, 0],
    ["向前", 0, -1],
    ["向后", 0, 1],
  ])
    action(label, () => {
      move(dx, 0, dz);
      draw();
    });
  for (const horizontal of [true, false])
    action(horizontal ? "横轴旋转" : "竖轴旋转", () => {
      const next = piece.map(([x, y, z]) =>
        horizontal ? [x, -z, y] : [z, y, -x],
      );
      if (!over && valid(next, origin)) piece = next;
      draw();
    });
  action("直接落下", () => {
    while (move(0, -1, 0)) {}
    lock();
    draw();
  });
  action("重新开始", () => reset(false));
  action("装入测试局面", () => reset(true));
  reset(false);
  tick = (dt) => {
    elapsed += dt;
    if (elapsed > 0.85 && !over) {
      elapsed = 0;
      if (!move(0, -1, 0)) lock();
      draw();
    }
  };
}
async function init() {
  if (id === "brain") {
    document.querySelector("header").textContent = "脑部可视化 · 解剖模板";
    await model("/games/brain_human.glb");
    return;
  }
  const games = await (await fetch("/mindsoul/catalog.json")).json();
  start(games.find((g) => g.id === id));
  const toggle = document.createElement("button");
  toggle.textContent = "暂停";
  toggle.onclick = () => {
    paused = !paused;
    toggle.textContent = paused ? "继续" : "暂停";
  };
  menu.append(toggle);
  if (location.search === "?spatial" && id !== "blocks") {
    action("放入空间", () => {
      const target = objects.children.findLast(o => o.userData.placement);
      if (!target) return;
      const {file, scale} = target.userData.placement;
      location.href = "mindsoul-game://place?file=" + encodeURIComponent(file) + "&scale=" + scale;
    });
  }
}
init().catch((e) => (status.textContent = "场景加载失败"));
new ResizeObserver(() => {
  renderer.setSize(stage.clientWidth, stage.clientHeight);
  camera.aspect = stage.clientWidth / stage.clientHeight;
  camera.updateProjectionMatrix();
}).observe(stage);
let previous = performance.now();
renderer.setAnimationLoop((now) => {
  const dt = Math.min(0.08, (now - previous) / 1000);
  previous = now;
  if (!paused) {
    time += dt;
    tick(dt);
    mixers.forEach((m) => m.update(dt));
  }
  renderer.render(scene, camera);
});
