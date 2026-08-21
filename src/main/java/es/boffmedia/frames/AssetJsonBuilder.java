package es.boffmedia.frames;

public final class AssetJsonBuilder {

    private AssetJsonBuilder() {}

    public static String buildBlockymodel(String baseName, int sizeX, int sizeY, int yPosition, int zPosition, int offsetX, int offsetY, int offsetZ) {
        return "{\n" +
                "  \"nodes\": [\n" +
                "    {\n" +
                "      \"id\": \"1\",\n" +
                "      \"name\": \"cube\",\n" +
                "      \"position\": {\"x\": 0, \"y\": " + yPosition + ", \"z\": " + zPosition + "},\n" +
                "      \"orientation\": {\"x\": 0, \"y\": 0, \"z\": 0, \"w\": 1},\n" +
                "      \"shape\": {\n" +
                "        \"type\": \"box\",\n" +
                "        \"offset\": {\"x\": " + offsetX + ", \"y\": " + offsetY + ", \"z\": " + offsetZ + "},\n" +
                "        \"stretch\": {\"x\": 1, \"y\": 1, \"z\": 1},\n" +
                "        \"settings\": {\n" +
                "          \"isPiece\": false,\n" +
                "          \"size\": {\"x\": " + sizeX + ", \"y\": " + sizeY + ", \"z\": 2},\n" +
                "          \"isStaticBox\": true\n" +
                "        },\n" +
                "        \"textureLayout\": {\n" +
                "          \"back\": { \"offset\": { \"x\": 0, \"y\": 0 }, \"mirror\": { \"x\": false, \"y\": false }, \"angle\": 0 },\n" +
                "          \"right\": { \"offset\": { \"x\": 0, \"y\": 0 }, \"mirror\": { \"x\": false, \"y\": false }, \"angle\": 0 },\n" +
                "          \"front\": { \"offset\": { \"x\": 0, \"y\": 0 }, \"mirror\": { \"x\": false, \"y\": false }, \"angle\": 0 },\n" +
                "          \"left\": { \"offset\": { \"x\": 0, \"y\": 0 }, \"mirror\": { \"x\": false, \"y\": false }, \"angle\": 0 },\n" +
                "          \"top\": { \"offset\": { \"x\": 0, \"y\": 0 }, \"mirror\": { \"x\": false, \"y\": false }, \"angle\": 0 },\n" +
                "          \"bottom\": { \"offset\": { \"x\": 0, \"y\": 0 }, \"mirror\": { \"x\": false, \"y\": false }, \"angle\": 0 }\n" +
                "        },\n" +
                "        \"unwrapMode\": \"custom\",\n" +
                "        \"visible\": true,\n" +
                "        \"doubleSided\": false,\n" +
                "        \"shadingMode\": \"flat\"\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"format\": \"prop\",\n" +
                "  \"lod\": \"auto\"\n" +
                "}\n";
    }

    public static String buildItemJson(String baseName, String texturePath, float scaleFactor) {
        // Keep the dynamic item structure aligned with Luisca's working Boff_Frame_1x1
        // definition. In particular, State.Definitions is required by the frame
        // interaction/state pipeline; a minimal BlockType JSON can render initially
        // but is not stable after an asset rebuild.
        return "{\n" +
                "  \"TranslationProperties\": {\n" +
                "    \"Name\": \"frames." + baseName + ".name\",\n" +
                "    \"Description\": \"frames." + baseName + ".description\"\n" +
                "  },\n" +
                "  \"Categories\": [\"Blocks.Deco\"],\n" +
                "  \"BlockType\": {\n" +
                "    \"InteractionHint\": \"frames.use_hint\",\n" +
                "    \"Material\": \"Solid\",\n" +
                "    \"DrawType\": \"Model\",\n" +
                "    \"Opacity\": \"Transparent\",\n" +
                "    \"CustomModel\": \"Blocks/Frames/" + baseName + ".blockymodel\",\n" +
                "    \"Flags\": { \"IsUsable\": true },\n" +
                "    \"CustomModelTexture\": [ { \"Texture\": \"" + texturePath + "\" } ],\n" +
                "    \"HitboxType\": \"Painting\",\n" +
                "    \"VariantRotation\": \"NESW\",\n" +
                "    \"Gathering\": { \"Soft\": { \"IsWeaponBreakable\": false } },\n" +
                "    \"BlockParticleSetId\": \"Wood\",\n" +
                "    \"BlockSoundSetId\": \"Wood\",\n" +
                "    \"Support\": { \"North\": [ { \"FaceType\": \"Full\" } ] },\n" +
                "    \"ParticleColor\": \"#684127\",\n" +
                "    \"State\": {\n" +
                "      \"Definitions\": {\n" +
                "        \"1\": {\n" +
                "          \"InteractionHint\": \"frames.use_hint\",\n" +
                "          \"CustomModelTexture\": [ { \"Texture\": \"" + texturePath + "\" } ]\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"CubeShadingMode\": \"Standard\",\n" +
                "    \"Interactions\": { \"Use\": { \"Interactions\": [ { \"Type\": \"Frames_UseFrameInteraction\" } ] } },\n" +
                "    \"CustomModelScale\": " + scaleFactor + "\n" +
                "  },\n" +
                "  \"PlayerAnimationsId\": \"Block\",\n" +
                "  \"IconProperties\": { \"Scale\": 0.68, \"Rotation\": [22.5, 45, 22.5], \"Translation\": [8.5, -19.7] },\n" +
                "  \"ResourceTypes\": [ { \"Id\": \"Fuel\" }, { \"Id\": \"Charcoal\" } ],\n" +
                "  \"Tags\": { \"Type\": [\"Furniture\"] },\n" +
                "  \"Icon\": \"Icons/ItemsGenerated/Boff_Frame_1x1.png\"\n" +
                "}\n";
    }
}
