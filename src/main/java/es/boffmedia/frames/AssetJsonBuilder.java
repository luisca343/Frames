package es.boffmedia.frames;

public final class AssetJsonBuilder {

    private AssetJsonBuilder() {}

    public static String buildBlockymodel(String baseName, int sizeX, int sizeY, int yOffset, int zOffset) {
        return "{\n" +
                "  \"nodes\": [\n" +
                "    {\n" +
                "      \"id\": \"1\",\n" +
                "      \"name\": \"cube\",\n" +
                "      \"position\": {\"x\": 0, \"y\": " + yOffset + ", \"z\": " + zOffset + "},\n" +
                "      \"orientation\": {\"x\": 0, \"y\": 0, \"z\": 0, \"w\": 1},\n" +
                "      \"shape\": {\n" +
                "        \"type\": \"box\",\n" +
                "        \"offset\": {\"x\": 0, \"y\": 0, \"z\": 0},\n" +
                "        \"stretch\": {\"x\": 1, \"y\": 1, \"z\": 1},\n" +
                "        \"settings\": {\n" +
                "          \"isPiece\": false,\n" +
                "          \"size\": {\"x\": " + sizeX + ", \"y\": " + sizeY + ", \"z\": 2},\n" +
                "          \"isStaticBox\": true\n" +
                "        },\n" +
                "        \"textureLayout\": {\n" +
                "          \"back\": { \"angle\": 0 },\n" +
                "          \"right\": { \"angle\": 0 },\n" +
                "          \"front\": { \"angle\": 0 },\n" +
                "          \"left\": { \"angle\": 0 },\n" +
                "          \"top\": { \"angle\": 0 }\n" +
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
        return "{\n" +
                "  \"TranslationProperties\": {\n" +
                "    \"Name\": \"frames." + baseName + ".name\",\n" +
                "    \"Description\": \"frames." + baseName + ".description\"\n" +
                "  },\n" +
                "  \"Categories\": [\n" +
                "    \"Blocks.Deco\"\n" +
                "  ],\n" +
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
                "    \"BlockParticleSetId\": \"Wood\",\n" +
                "    \"BlockSoundSetId\": \"Wood\",\n" +
                "    \"ParticleColor\": \"#684127\",\n" +
                "    \"Interactions\": { \"Use\": { \"Interactions\": [ { \"Type\": \"Frames_UseFrameInteraction\" } ] } },\n" +
                "    \"CustomModelScale\": " + scaleFactor + "\n" +
                "  },\n" +
                "  \"PlayerAnimationsId\": \"Block\",\n" +
                "  \"IconProperties\": { \"Scale\": 0.68, \"Rotation\": [22.5, 45, 22.5], \"Translation\": [8.5, -19.7] },\n" +
                "  \"ResourceTypes\": [],\n" +
                "  \"Tags\": {},\n" +
                "  \"Icon\": \"Icons/ItemsGenerated/Boff_Frame_1x1.png\",\n" +
                "  \"DropOnBreak\": \"Boff_Frame_1x1\"\n" +
                "}\n";
    }
}
