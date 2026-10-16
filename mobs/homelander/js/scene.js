/* Homelander viewer, part 'scene': Renderer, scene, camera, lights.
   Classic script (loaded in order by homelander.html; THREE is global). */
/* ───────── Renderer, scene, camera ───────── */
const canvas = document.getElementById('viewport');
const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, powerPreference: 'high-performance' });
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
renderer.setSize(window.innerWidth, window.innerHeight);
renderer.shadowMap.enabled = true;
renderer.shadowMap.type = THREE.PCFSoftShadowMap;
renderer.outputColorSpace = THREE.SRGBColorSpace;
renderer.toneMapping = THREE.ACESFilmicToneMapping;
renderer.toneMappingExposure = 1.15;

const scene = new THREE.Scene();
scene.background = new THREE.Color(0x0f1115);
scene.fog = new THREE.FogExp2(0x0f1115, 0.016);

const TEXEL = 1 / 16; // 1 unit = 1 texel, 16 texels per Minecraft block (1m)

const CAM_POS = new THREE.Vector3(2.4, 1.75, 2.9);
const CAM_TARGET = new THREE.Vector3(0, 1.05, 0);
const camera = new THREE.PerspectiveCamera(36, window.innerWidth / window.innerHeight, 0.1, 60);
camera.position.copy(CAM_POS);

const controls = new OrbitControls(camera, renderer.domElement);
controls.target.copy(CAM_TARGET);
controls.enableDamping = true;
controls.minDistance = 1.2;
controls.maxDistance = 7.5;
controls.maxPolarAngle = Math.PI * 0.495;
controls.update();

/* ───────── Lighting ───────── */
scene.add(new THREE.HemisphereLight(0xfff5ea, 0x141824, 0.95));

const keyLight = new THREE.DirectionalLight(0xfff8ee, 2.45);
keyLight.position.set(3.6, 6.2, 4.2);
keyLight.castShadow = true;
keyLight.shadow.mapSize.set(2048, 2048);
keyLight.shadow.camera.near = 0.5;
keyLight.shadow.camera.far = 18;
keyLight.shadow.camera.left = keyLight.shadow.camera.bottom = -2.5;
keyLight.shadow.camera.right = keyLight.shadow.camera.top = 2.5;
keyLight.shadow.bias = -0.0005;
keyLight.shadow.radius = 3.2;
scene.add(keyLight);

const rimLight = new THREE.DirectionalLight(0x60a5fa, 0.95);
rimLight.position.set(-3.6, 3.2, -3.6);
scene.add(rimLight);

const fillLight = new THREE.DirectionalLight(0xfef08a, 0.38);
fillLight.position.set(-2.6, 1.5, 3.0);
scene.add(fillLight);

const ground = new THREE.Mesh(new THREE.PlaneGeometry(24, 24), new THREE.ShadowMaterial({ opacity: 0.45 }));
ground.rotation.x = -Math.PI / 2;
ground.receiveShadow = true;
scene.add(ground);
