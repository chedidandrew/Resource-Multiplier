# Fabric placement-provenance fixture

`fabric-placement-provenance-chunk--554625--233041.nbt.b64` is the Base64
encoding of an uncompressed chunk NBT read from a freshly saved Fabric 1.21.11
GameTest world on 2026-09-01. It came from chunk slot 511 in
`r.-17333.-7283.mca`, corresponding to chunk `[-554625, -233041]`, and retains
Minecraft 1.21.11 data version `4671`.

The fresh Fabric chunk was augmented with the exact persistent envelope emitted
by Fabric Data Attachment API 1.8.48 for
`fabric:attachments.smart_resource_drops:placed_blocks`, using the mod's
`PlacedBlockData.CODEC` and packed position `-13958`. It contains no NeoForge
attachment envelope. Its decoded size is 10,861 bytes and its SHA-256 is
`c390fc16519a7b9f9a1fc29feab66209bca96b5db8bff6e659b239d16d36a38d`.

The fixture is Base64 rather than binary so its exact bytes remain reviewable
and portable through text-only patches. This is a version-correct serialization
fixture, not a claim that the marked position came from a physical player
placement. The regression test imports it, writes the resulting native
attachment into a temporary Anvil region, closes and reopens that region, then
loads the native attachment into a fresh chunk. Do not regenerate or hand-edit
it from NeoForge output: the test depends on the outer chunk remaining a Fabric
1.21.11 world artifact and on the legacy envelope remaining Fabric-format data.
