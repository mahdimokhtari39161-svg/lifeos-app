# آخرین پناهگاه 3D — Last Shelter Tactical FPS

A mobile-first browser 3D first-person survival shooter prototype built with Three.js.

## Play
https://mahdimokhtari39161-svg.github.io/lifeos-app/survival-game/

## Controls
- **Mobile:** left touch joystick to move; drag the right side to look; hold FIRE to shoot; aim button to zoom; reload and sprint buttons.
- **Desktop:** WASD to move, mouse to look, left-click to fire, R to reload, Shift to sprint.

## Current features
- 3D fortified desert outpost, perimeter barriers, guard towers, terrain, trees, props, atmospheric fog, shadows and directional lighting.
- Animated rigged 3D soldier model with Idle / Walk / Run clips, loaded from the Three.js example model library; fallback procedural soldier remains available if the asset host fails.
- First-person carbine with receiver, handguard, barrel, stock, magazine, iron sights, first-person arms/gloves, recoil, weapon bob, reload magazine animation, muzzle flare and tracer.
- Enemy AI advances toward the player, changes locomotion animation by distance and fires at range; close-range attacks, hit reaction and falling death.
- Aim zoom, touch controls, ammo, health, wave progress, pause, and local save.

## Third-party asset notice
The animated Soldier GLB is loaded from the official Three.js examples repository:
https://github.com/mrdoob/three.js/tree/r164/examples/models/gltf
Three.js is distributed under the MIT License. This project is an original prototype and is not affiliated with or endorsed by Call of Duty.

## Notes
This is an enhanced playable prototype, not a finished AAA game. It uses an animated game-ready sample soldier, not photorealistic scans. The first load needs an internet connection to load Three.js and the external GLB. Progress is stored locally on the same device.
