# Third-party notices and modifications

Lunaciia's Greenhouse is extracted from the LUS Greenhouse module. Copyright
(c) 2026 Lunaciia and Sanctuary Orbs contributors. The combined work is
GPL-3.0-only (LICENSE). Earlier LUS MIT attribution is retained in
licenses/LUS-MIT.txt. This project is AI-made; that description does not claim
authorship of the upstream code, crop artwork or data listed below.

## SkyShards Greenhouse

Copyright (c) 2026 Campion, MIT. Full terms:
licenses/SkyShards-Greenhouse-MIT.txt.
https://github.com/Campionnn/SkyShards-Greenhouse/tree/78240b5fe76d2d8f898544f10b3a3f3e16cf4890

Crop PNGs and catalog metadata derive from this source. GreenhouseCode is a
modified Java port of designEncoding.ts and cropMapping.ts. The Java editor,
soil editing and local preset library replace the web UI. No web simulation
engine or JavaScript runtime is bundled.

## Skyblocker

Copyright Skyblocker contributors, LGPL-3.0. Full terms:
licenses/Skyblocker-LGPL-3.0.txt.
https://github.com/SkyblockerMod/Skyblocker/tree/64fd061b9eee19cdd2392d3baf96ef780acc6435

GreenhouseCrops, GreenhousePaste and HeadTextures supplied crop recognition,
head texture identities, Garden grid origin and large crop offsets. Modified
implementations: GreenhouseCatalog, GreenhouseProjection and head-textures.json.
Rendering and scan integration were replaced by bounded client tick scanning
and native Fabric rendering. Modified source is included in this repository.
Litematica was a conceptual reference for expected/actual overlay states;
no Litematica code is copied or bundled.

## Hypixel Mod API

Copyright (c) 2024 Hypixel Inc., MIT. Full terms:
licenses/Hypixel-ModAPI-MIT.txt. The Fabric adapter is bundled as a nested mod
(Modrinth version SlqCF6Or); its metadata and dependency notices are retained.
https://github.com/HypixelDev/ModAPI

Used only for confirmed Garden location events. LUS, Skyblocker and a separate
Hypixel Mod API installation are not required. Fabric API remains an external
dependency, distributed by its authors.

## Reference boundaries

The extracted behavior retains the existing implementation's source snapshot.
Local knowledge reference: Greenhouse revision 857525 (2026-10-13),
https://hypixelskyblock.minecraft.wiki/?oldid=857525 . No Wiki corpus is bundled.
This is a planting-layout editor and passive projection, not a growth simulator
or automatic planter. Loaded client data and current source recognition limit
projection results; it cannot read unloaded crops.
