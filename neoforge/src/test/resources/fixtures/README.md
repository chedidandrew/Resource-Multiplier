# Fabric placement-provenance fixture

`fabric-placement-provenance-chunk--435018--913934.nbt.b64` is the Base64
encoding of the exact uncompressed chunk NBT captured from the Fabric 26.2
GameTest world on 2026-09-01. It came from chunk slot 598 in
`r.-13595.-28561.mca`, corresponding to chunk `[-435018, -913934]`.

The captured chunk contains Fabric's persisted
`fabric:attachments.smart_resource_drops:placed_blocks` value with packed
position `-13958` and contains no NeoForge attachment envelope. Its decoded
size is 11,088 bytes and its SHA-256 is
`b36540e977c8dd932e8a2841787657cc74cf3e3e2029b50c9e3c05877ba07690`.

The fixture is Base64 rather than binary so its exact bytes remain reviewable
and portable through text-only patches. The regression test imports it, writes
the resulting native attachment into a temporary Anvil region, closes and
reopens that region, then loads the native attachment into a fresh chunk. Do
not regenerate or hand-edit it from NeoForge output: the test depends on this
remaining a Fabric-authored artifact.
