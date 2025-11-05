package io.github.orlouge.landmarks.generation;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BlockTemplate {
    public abstract BlockState getBlockState(StructureWorldAccess world, Random random, BiFunction<String, Boolean, BlockTemplate> palette);
    public abstract void process(StructureWorldAccess world, Random random, BlockPos pos, Direction direction);
    public abstract Collection<String> getReferencedPaletteEntries();
    public abstract BlockTemplate copy();

    public static BlockTemplate empty() {
        return new Simple(null);
    }

    public boolean isEmpty() { return false; }

    public static BlockTemplate block(BlockState state) {
        return new Simple(state);
    }

    public static BlockTemplate block(Block block) {
        return block(block.getDefaultState());
    }

    public static BlockTemplate parse(String blockArgument) {
        return new RandomChoiceParsing(blockArgument);
        /*
        String[] options = blockArgument.split(",");
        if (options.length == 1) {
            return new SimpleParsing(blockArgument);
        } else {
            List<BlockTemplate> templates = new ArrayList<>();
            int emptyWeight = 0;
            for (String arg : options) {
                if (arg.isEmpty()) {
                    emptyWeight += 1;
                } else {
                    templates.add(new SimpleParsing(arg));
                }
            }
            return new RandomChoice(templates, emptyWeight);
        }
         */
    }

    public static BlockTemplate random(BlockState[] states) {
        return new RandomChoice(Arrays.stream(states).map(Simple::new).map(s -> (BlockTemplate) s).toList(), 0);
    }

    public static BlockTemplate random(BlockState[] states, int emptyWeight) {
        return new RandomChoice(Arrays.stream(states).map(Simple::new).map(s -> (BlockTemplate) s).toList(), emptyWeight);
    }

    /*
    public static BlockTemplate lootContainer(BlockState state, RegistryKey<LootTable> lootTableId) {
        return new ProcessBlockEntity(new Simple(state), (entity, random) -> {
            if (entity instanceof LootableContainerBlockEntity) {
                ((LootableContainerBlockEntity) entity).setLootTable(lootTableId, random.nextLong());
            }
        });
    }

    public static BlockTemplate lootContainer(Block block, RegistryKey<LootTable> lootTableId) {
        return lootContainer(block.getDefaultState(), lootTableId);
    }

    public static BlockTemplate lootContainer(BlockState state, Identifier lootTable) {
        return lootContainer(state, RegistryKey.of(RegistryKeys.LOOT_TABLE, lootTable));
    }

    public static BlockTemplate lootBrushable(BlockState state, RegistryKey<LootTable> lootTableId) {
        return new ProcessBlockEntity(new Simple(state), (entity, random) -> {
            if (entity instanceof BrushableBlockEntity) {
                ((BrushableBlockEntity) entity).setLootTable(lootTableId, random.nextLong());
            }
        });
    }

    public static BlockTemplate lootBrushable(Block block, Identifier lootTableId) {
        return lootBrushable(block, RegistryKey.of(RegistryKeys.LOOT_TABLE, lootTableId));
    }

    public static BlockTemplate lootBrushable(Block block, RegistryKey<LootTable> lootTableId) {
        return lootBrushable(block.getDefaultState(), lootTableId);
    }

    public static BlockTemplate blockEntity(BlockTemplate template, BiConsumer<BlockEntity, Random> blockEntityFunction) {
        return new ProcessBlockEntity(template, blockEntityFunction);
    }

    public static BlockTemplate blockEntity(BlockState state, BiConsumer<BlockEntity, Random> blockEntityFunction) {
        return blockEntity(new Simple(state), blockEntityFunction);
    }

    public static BlockTemplate blockEntity(Block block, BiConsumer<BlockEntity, Random> blockEntityFunction) {
        return blockEntity(block.getDefaultState(), blockEntityFunction);
    }

    public static BlockTemplate sideEffect(BlockTemplate template, Consumer<ProcessContext> processor) {
        return new ProcessWorld(template, processor);
    }

    public static BlockTemplate sideEffect(BlockTemplate template, BiConsumer<StructureWorldAccess, BlockPos> processor) {
        return new ProcessWorld(template, ctx -> processor.accept(ctx.world, ctx.pos));
    }

    public static BlockTemplate sideEffect(BlockState state, BiConsumer<StructureWorldAccess, BlockPos> processor) {
        return sideEffect(new Simple(state), processor);
    }

     */

    public abstract BlockTemplateType<?> getType();

    private static class Simple extends BlockTemplate {
        private final BlockState state;

        public Simple(BlockState state) {
            this.state = state;
        }

        @Override
        public BlockState getBlockState(StructureWorldAccess world, Random random, BiFunction<String, Boolean, BlockTemplate> palette) {
            return state;
        }

        @Override
        public void process(StructureWorldAccess world, Random random, BlockPos pos, Direction direction) {
        }

        @Override
        public boolean isEmpty() {
            return state == null;
        }

        @Override
        public Collection<String> getReferencedPaletteEntries() {
            return List.of();
        }

        @Override
        public BlockTemplate copy() {
            return new Simple(state);
        }

        @Override
        public BlockTemplateType<?> getType() {
            return SIMPLE_TEMPLATE_TYPE;
        }

        @Override
        public String toString() {
            return state == null ? "*" : state.toString().replaceAll("Block\\{(minecraft:)?([^{}]*)}", "$2");
        }
    }

    private static class SimpleParsing extends BlockTemplate {
        private final String blockString;
        private BlockState state = null;
        private boolean parseAttempt = false;

        public SimpleParsing(String blockString) {
            this.blockString = blockString;
        }

        @Override
        public BlockState getBlockState(StructureWorldAccess world, Random random, BiFunction<String, Boolean, BlockTemplate> palette) {
            if (!parseAttempt && state == null) {
                parseAttempt = true;
                try {
                    if (blockString.startsWith("*")) {
                        state = null;
                    } else if (blockString.startsWith("%")) {
                        BlockTemplate template = palette.apply(blockString.substring(1), true);
                        state = template == null ? null : template.getBlockState(world, random, palette);
                    } else {
                        BlockArgumentParser.BlockResult result = BlockArgumentParser.block(world.createCommandRegistryWrapper(RegistryKeys.BLOCK), blockString, false);
                        state = result.blockState();
                    }
                } catch (CommandSyntaxException e) {
                    return null;
                }
            }
            return state;
        }

        @Override
        public void process(StructureWorldAccess world, Random random, BlockPos pos, Direction direction) {
        }

        @Override
        public Collection<String> getReferencedPaletteEntries() {
            return blockString.startsWith("%*") ? List.of(blockString.substring(2)) : List.of();
        }

        @Override
        public BlockTemplate copy() {
            return new SimpleParsing(blockString);
        }

        @Override
        public BlockTemplateType<?> getType() {
            return SIMPLE_PARSING_TEMPLATE_TYPE;
        }

        @Override
        public String toString() {
            return parseAttempt ? (state == null ? "*" : state.toString().replaceAll("Block\\{(minecraft:)?([^{}]*)}", "$2")) : "\"" + blockString + "\"";
        }
    }

    private static class RandomChoiceParsing extends BlockTemplate {
        private final String blockString;
        private BlockTemplate template = null;
        private boolean parseAttempt = false;
        private HashSet<String> referencedPaletteEntries = null;

        public RandomChoiceParsing(String blockString) {
            this.blockString = blockString;
        }

        @Override
        public BlockState getBlockState(StructureWorldAccess world, Random random, BiFunction<String, Boolean, BlockTemplate> palette) {
            if (!parseAttempt && template == null) {
                parseAttempt = true;
                parse(palette);
            }
            return template == null ? null : template.getBlockState(world, random, palette);
        }

        private void parse(BiFunction<String, Boolean, BlockTemplate> palette) {
            referencedPaletteEntries = new HashSet<>();
            String[] options = blockString.split(";");
            if (options.length == 1 && !options[0].contains("?")) {
                template = new SimpleParsing(blockString);
                referencedPaletteEntries.addAll(template.getReferencedPaletteEntries());
            } else {
                List<BlockTemplate> templates = new ArrayList<>();
                int emptyWeight = 0;
                for (String arg : options) {
                    BlockTemplate subTemplate = null;
                    int weight = 1;

                    if (arg.isEmpty() || arg.startsWith("*")) {
                        emptyWeight += arg.length() > 1 ? Integer.parseInt(arg.substring(1)) : 1;
                    } else {
                        String[] weightSplit = arg.split("\\*");
                        if (weightSplit.length == 2) {
                            arg = weightSplit[0];
                            weight = Integer.parseInt(weightSplit[1]);
                        }

                        if (arg.startsWith("%?") || arg.startsWith("%%?")) {
                            String reference = arg.startsWith("%?") ? arg.substring(2) : "%" + arg.substring(3);
                            referencedPaletteEntries.add(reference);
                            subTemplate = palette.apply(reference, false);
                            if (subTemplate.isEmpty()) subTemplate = null;
                            // if (subTemplate != null) referencedPaletteEntries.addAll(subTemplate.getReferencedPaletteEntries());
                        } else {
                            subTemplate = new SimpleParsing(arg);
                            referencedPaletteEntries.addAll(subTemplate.getReferencedPaletteEntries());
                        }
                    }

                    if (subTemplate != null) {
                        for (int i = 0; i < weight; i++) templates.add(subTemplate);
                    }
                }
                template = new RandomChoice(templates, emptyWeight);
            }
        }

        @Override
        public void process(StructureWorldAccess world, Random random, BlockPos pos, Direction direction) {
        }

        @Override
        public Collection<String> getReferencedPaletteEntries() {
            // TODO
            return Collections.emptySet();
            /*
            if (referencedPaletteEntries == null) parse();
            return referencedPaletteEntries;

             */
        }

        @Override
        public BlockTemplate copy() {
            return new RandomChoiceParsing(blockString);
        }

        @Override
        public BlockTemplateType<?> getType() {
            return RANDOM_PARSING_TEMPLATE_TYPE;
        }

        @Override
        public String toString() {
            return parseAttempt ? (template != null ? template.toString() : "*") : "\"" + blockString + "\"";
        }
    }

    private static class RandomChoice extends BlockTemplate {
        private final List<BlockTemplate> choices;
        private final int emptyWeight;

        public RandomChoice(List<BlockTemplate> choices, int emptyWeight) {
            this.choices = choices;
            this.emptyWeight = emptyWeight;
        }

        @Override
        public BlockState getBlockState(StructureWorldAccess world, Random random, BiFunction<String, Boolean, BlockTemplate> palette) {
            int maxChoice = this.choices.size() + emptyWeight;
            int choice = maxChoice > 0 ? random.nextInt(maxChoice) : 0;
            return choice < this.choices.size() ? this.choices.get(choice).getBlockState(world, random, palette) : null;
        }

        @Override
        public void process(StructureWorldAccess world, Random random, BlockPos pos, Direction direction) {
        }

        @Override
        public Collection<String> getReferencedPaletteEntries() {
            return choices.stream().flatMap(t -> t.getReferencedPaletteEntries().stream()).collect(Collectors.toSet());
        }

        @Override
        public BlockTemplate copy() {
            return new RandomChoice(choices, emptyWeight);
        }

        @Override
        public BlockTemplateType<?> getType() {
            return RANDOM_CHOICE_TEMPLATE_TYPE;
        }

        @Override
        public String toString() {
            Map<String, Integer> cnt = new HashMap<>();
            for (BlockTemplate template : this.choices) {
                cnt.merge(template.toString(), 1, Integer::sum);
            }
            if (emptyWeight > 0) cnt.merge("*", emptyWeight, Integer::sum);
            cnt.merge("", cnt.getOrDefault("*", 0), Integer::sum);
            cnt.remove("*");
            if (cnt.get("") == 0) cnt.remove("");
            return "(" + String.join(";", cnt.entrySet().stream().map(e -> e.getKey() + (e.getValue() == 1 && !e.getKey().isEmpty() ? "" : "*" + e.getValue())).toList()) + ")";
        }
    }

    /*
    private static class ProcessWorld extends BlockTemplate {
        private final BlockTemplate template;
        private final Consumer<ProcessContext> worldFunction;

        private ProcessWorld(BlockTemplate template, Consumer<ProcessContext> worldFunction) {
            this.template = template;
            this.worldFunction = worldFunction;
        }

        @Override
        public BlockState getBlockState(StructureWorldAccess world, Random random) {
            return template.getBlockState(world, random);
        }

        @Override
        public void process(StructureWorldAccess world, Random random, BlockPos pos, Direction direction) {
            this.worldFunction.accept(new ProcessContext(world, random, pos, direction));
        }
    }

    public record ProcessContext(StructureWorldAccess world, Random random, BlockPos pos, Direction direction) {}

    private static class ProcessBlockEntity extends BlockTemplate {
        private final BlockTemplate baseTemplate;
        private final BiConsumer<BlockEntity, Random> blockEntityFunction;

        private ProcessBlockEntity(BlockTemplate baseTemplate, BiConsumer<BlockEntity, Random> blockEntityFunction) {
            this.baseTemplate = baseTemplate;
            this.blockEntityFunction = blockEntityFunction;
        }

        @Override
        public BlockState getBlockState(StructureWorldAccess world, Random random) {
            return baseTemplate.getBlockState(world, random);
        }

        @Override
        public void process(StructureWorldAccess world, Random random, BlockPos pos, Direction direction) {
            BlockEntity entity = world.getBlockEntity(pos);
            if (entity != null) this.blockEntityFunction.accept(entity, random);
        }
    }

     */

    public record BlockTemplateType<T extends BlockTemplate>(String id, MapCodec<T> codec) {}


    private static final Map<String, BlockTemplateType<?>> TEMPLATE_TYPES = new HashMap<>();

    private static final Codec<BlockTemplateType<?>> TEMPLATE_TYPE_CODEC = Codec.STRING.xmap(
        TEMPLATE_TYPES::get, BlockTemplateType::id
    );

    private static final Codec<BlockTemplate> RECORD_CODEC =
        TEMPLATE_TYPE_CODEC.dispatch("type", BlockTemplate::getType, BlockTemplateType::codec);

    public static final Codec<BlockTemplate> CODEC = Codec.either(Codec.STRING, RECORD_CODEC).xmap(
        either -> either.map(BlockTemplate::parse, t -> t),
        template -> template instanceof RandomChoiceParsing parsing ? Either.left(parsing.blockString) : Either.right(template)
    );

    private static final BlockTemplateType<Simple> SIMPLE_TEMPLATE_TYPE = new BlockTemplateType<>("block", RecordCodecBuilder.mapCodec(instance -> instance.group(
        BlockState.CODEC.fieldOf("state").forGetter(t -> t.state)
    ).apply(instance, Simple::new)));
    private static final BlockTemplateType<SimpleParsing> SIMPLE_PARSING_TEMPLATE_TYPE = new BlockTemplateType<>("block_argument", RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.fieldOf("argument").forGetter(t -> t.blockString)
    ).apply(instance, SimpleParsing::new)));
    private static final BlockTemplateType<RandomChoiceParsing> RANDOM_PARSING_TEMPLATE_TYPE = new BlockTemplateType<>("random_block_argument", RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.fieldOf("argument").forGetter(t -> t.blockString)
    ).apply(instance, RandomChoiceParsing::new)));
    private static final BlockTemplateType<RandomChoice> RANDOM_CHOICE_TEMPLATE_TYPE = new BlockTemplateType<>("random", RecordCodecBuilder.mapCodec(instance -> instance.group(
        CODEC.listOf().fieldOf("choices").forGetter(t -> t.choices),
        Codec.INT.fieldOf("emptyWeight").forGetter(t -> t.emptyWeight)
    ).apply(instance, RandomChoice::new)));

    static {
        TEMPLATE_TYPES.put(SIMPLE_TEMPLATE_TYPE.id, SIMPLE_TEMPLATE_TYPE);
        TEMPLATE_TYPES.put(RANDOM_CHOICE_TEMPLATE_TYPE.id, RANDOM_CHOICE_TEMPLATE_TYPE);
    }
}
