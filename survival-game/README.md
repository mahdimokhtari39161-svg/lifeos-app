# آخرین پناهگاه 3D — Last Shelter Tactical FPS

A mobile-first 3D first-person survival shooter prototype using Three.js.

## Play
https://mahdimokhtari39161-svg.github.io/lifeos-app/survival-game/

## Character selection
Choose one of eight playable classes before entering a mission. Each class has different health, movement speed, damage, magazine capacity, reserve ammunition, reload speed, fire rate, weapon designation and a special ability.

| Class | Role | Special ability |
|---|---|---|
| Soldier | Balanced infantry | Combat resupply: heal and ammunition |
| Sniper | Long-range damage | Deadly focus: temporary damage boost |
| Commander | Field leader | Command pulse: temporarily freezes enemy movement and attacks |
| Assassin | Fast infiltrator | Shadow stealth: sharply reduces enemy hit chance |
| Operator | Assault specialist | Rapid fire: faster firing for a short time |
| Medic | Survival support | Immediate healing and passive regeneration |
| Heavy | Armored frontline | Reinforced armor: reduces incoming damage |
| Elite | Special forces | Overdrive: increases firing speed and damage |

## Controls
- **Mobile:** left touch joystick to move; drag the right side to look; hold FIRE to shoot; aim button to zoom; reload and sprint buttons; SPECIAL activates the selected class ability.
- **Desktop:** WASD to move, mouse to look, left-click to fire, R to reload, Shift sprint. Use SPECIAL for the class ability.

## Current features
- 3D fortified desert outpost, guard towers, terrain, trees, cover props, atmospheric fog, shadows and lighting.
- Eight portrait choices based on the supplied character reference image.
- Eight playable gameplay classes with individual health, speed, damage, magazine, reserves, reload and fire-rate values.
- A special ability and cooldown for every class; medic regeneration, commander crowd-control, assassin stealth and heavy damage reduction are implemented.
- Animated rigged 3D soldier model with Idle / Walk / Run clips and a procedural fallback if the external asset is unavailable.
- First-person carbine, recoil, movement sway, animated reload, muzzle flare and shot tracers.
- Enemy AI, enemy fire, hit reactions, falling death animation, waves, health/ammo HUD, pause and local save.
- Offline app shell and cached character portrait atlas after the first successful load.

## Asset and scope notes
- Character portraits are cropped from the user-provided character selection reference artwork. They are selection portraits, not eight independent photorealistic 3D character models.
- The animated Soldier GLB is loaded from the Three.js example model library: https://github.com/mrdoob/three.js/tree/r164/examples/models/gltf
- This is an enhanced playable prototype, not a finished AAA or multiplayer game. Separate rigged 3D bodies, unique fully modeled weapons, voice lines, advanced maps and network multiplayer remain future production work.
- First use requires an internet connection for Three.js and the external GLB asset. Progress is stored locally on the same device.

## Third-party project
Three.js is distributed under the MIT License. The project is an original prototype and is not affiliated with or endorsed by Call of Duty.


## Iranian Army defenders update
- Enemy faction is represented as fictional Iranian Army Ground Forces-inspired defenders, with khaki/olive/brown DPM-style camouflage, field kit and small tricolour identifiers.
- Defenders are stationed at posts inside the existing 144 x 80 m three-vault underground facility (20/40/20 m bays) and near both same-side access tunnels. They hold their positions and engage the player only after the player enters the facility/tunnel zone.
- The original structure footprint and room layout are preserved; defensive props are visual set dressing, not structural changes.
