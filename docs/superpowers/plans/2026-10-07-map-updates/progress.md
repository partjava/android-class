# Progress

## Latest status · 2026-10-07

Published documentation commits 17dfaab and c5a1141 to main, and published the campus-preview-2026-10-07 APK release with a direct download and verified SHA-256. That first APK used uncommitted working-tree changes at publication time. The follow-up R2 release points to committed source 268261c and includes the corresponding APK; its 46 campus assets were verified against the source working tree.

The committed implementation includes 212 default roads, south gate rotation -75°, default-relative building exports, round road joints, shared road rendering, endpoint/segment snapping, 100-step road undo/redo, road reset, connectivity diagnostics, and layout status with local backup restoration. The toolbar now uses a scrollable tool rail rather than the original More menu.

All 17 map checks, Android unit tests and debug assembly passed. Browser checks cover 320/420/1100px layouts, road resets, undo/redo, connectivity displays and layout switching/backup. Full device regression remains outstanding. README, feature inventory and usage instructions were synchronized with this status.

## Original implementation record

The statements below describe the earlier implementation checkpoint, before the later commits and APK release.

2026-10-07: inspected gesture dispatch, road editor pointer listeners and road graph source. Implementing all six user requested improvements in the existing checkout.

Completed midpoint anchored pinch, single pointer continuation, reversible draw/erase gestures during multitouch, top-view state and More menu. Added obstacle-aware road graph and route UI with route swap, clear, and failure messages. Verified actual default route combinations; 320/420/1100px browser tests used native touch dispatch for pinch and all three drawing modes. Geometry/interaction suite and offline Android debug build passed. Device testing and remote Git push have not been performed.
