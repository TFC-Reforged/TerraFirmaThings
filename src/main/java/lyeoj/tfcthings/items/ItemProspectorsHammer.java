package lyeoj.tfcthings.items;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.dries007.tfc.api.capability.forge.ForgeableHeatableHandler;
import net.dries007.tfc.api.capability.metal.IMetalItem;
import net.dries007.tfc.api.capability.player.CapabilityPlayerData;
import net.dries007.tfc.api.capability.size.Size;
import net.dries007.tfc.api.capability.size.Weight;
import net.dries007.tfc.api.types.Metal;
import net.dries007.tfc.objects.blocks.wood.BlockSupport;
import net.dries007.tfc.objects.items.ItemTFC;
import net.dries007.tfc.util.ICollapsableBlock;
import net.dries007.tfc.util.IFallingBlock;
import net.dries007.tfc.util.skills.ProspectingSkill;
import net.dries007.tfc.util.skills.SkillType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemProspectorsHammer extends ItemTFC implements IMetalItem {

    private final Metal metal;
    public final ToolMaterial material;
    private final double attackDamage;
    private final float attackSpeed;

    public ItemProspectorsHammer(Metal metal, String name) {
        this.metal = metal;
        this.material = metal.getToolMetal();
        this.setMaxDamage((int)((double)material.getMaxUses() / 4));
        this.attackDamage = (double)(0.5 * this.material.getAttackDamage());
        this.attackSpeed = -2.8F;
        this.setMaxStackSize(1);
        setRegistryName("prospectors_hammer/" + name);
        setTranslationKey("prospectors_hammer_" + name);
    }

    @Nonnull
    @Override
    public Size getSize(@Nonnull ItemStack itemStack) {
        return Size.NORMAL;
    }

    @Nonnull
    @Override
    public Weight getWeight(@Nonnull ItemStack itemStack) {
        return Weight.MEDIUM;
    }

    public boolean canStack(ItemStack itemStack) {
        return false;
    }

    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
        Multimap<String, AttributeModifier> multimap = HashMultimap.create();
        if (slot == EntityEquipmentSlot.MAINHAND) {
            multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Tool modifier", this.attackDamage, 0));
            multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, "Tool modifier", (double)this.attackSpeed, 0));
        }

        return multimap;
    }

    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack itemstack = playerIn.getHeldItem(handIn);
        RayTraceResult raytraceresult = this.rayTrace(worldIn, playerIn, false);
        if (raytraceresult == null) {
            return new ActionResult<ItemStack>(EnumActionResult.PASS, itemstack);
        } else if (raytraceresult.typeOfHit != RayTraceResult.Type.BLOCK) {
            return new ActionResult<ItemStack>(EnumActionResult.PASS, itemstack);
        } else {
            BlockPos blockpos = raytraceresult.getBlockPos();
            IBlockState iblockstate = worldIn.getBlockState(blockpos);
            SoundType soundType = iblockstate.getBlock().getSoundType(iblockstate, worldIn, blockpos, playerIn);
            worldIn.playSound(playerIn, blockpos, soundType.getHitSound(), SoundCategory.BLOCKS, 1.0F, soundType.getPitch());
            Block block = iblockstate.getBlock();
            if(!worldIn.isRemote) {
                ProspectingSkill skill = (ProspectingSkill) CapabilityPlayerData.getSkill(playerIn, SkillType.PROSPECTING);
                int messageType = 0;
                if(block instanceof ICollapsableBlock) {
                    boolean result = areNeighborsSupported(worldIn, blockpos);
                    float falsePositiveChance = 0.3F;
                    if (skill != null) {
                        falsePositiveChance = 0.3F - 0.1F * (float)skill.getTier().ordinal();
                    }
                    if(Math.random() < falsePositiveChance) {
                        result = true;
                    }
                    if(result) {
                        messageType = 1;
                    } else {
                        messageType = 2;
                    }
                }
                if(skill != null && skill.getTier().ordinal() > 1 && supportingFallable(worldIn, blockpos)) {
                    messageType += 3;
                }
                switch(messageType) {
                    case 0:
                        playerIn.sendMessage(new TextComponentTranslation("tfcthings.tooltip.prohammer_na", new Object[0]));
                        break;
                    case 1:
                        playerIn.sendMessage(new TextComponentTranslation("tfcthings.tooltip.prohammer_safe", new Object[0]));
                        break;
                    case 2:
                        playerIn.sendMessage(new TextComponentTranslation("tfcthings.tooltip.prohammer_unsafe", new Object[0]));
                        break;
                    case 3:
                        playerIn.sendMessage(new TextComponentTranslation("tfcthings.tooltip.prohammer_na_fall", new Object[0]));
                        break;
                    case 4:
                        playerIn.sendMessage(new TextComponentTranslation("tfcthings.tooltip.prohammer_safe_fall", new Object[0]));
                        break;
                    case 5:
                        playerIn.sendMessage(new TextComponentTranslation("tfcthings.tooltip.prohammer_unsafe_fall", new Object[0]));
                        break;
                }
                playerIn.getHeldItem(handIn).damageItem(1, playerIn);
            }
            return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, itemstack);
        }
    }

    private boolean areNeighborsSupported(World worldIn, BlockPos pos) {
        if(canThisBlockCollapse(worldIn, pos.down(), false)) {
            return false;
        }
        if(canThisBlockCollapse(worldIn, pos.up(), true)) {
            return false;
        }
        if(canThisBlockCollapse(worldIn, pos.north(), false)) {
            return false;
        }
        if(canThisBlockCollapse(worldIn, pos.south(), false)) {
            return false;
        }
        if(canThisBlockCollapse(worldIn, pos.east(), false)) {
            return false;
        }
        if(canThisBlockCollapse(worldIn, pos.west(), false)) {
            return false;
        }
        return true;
    }

    private boolean canThisBlockCollapse(World worldIn, BlockPos pos, boolean above) {
        IBlockState iblockstate = worldIn.getBlockState(pos);
        if(iblockstate.getBlock() instanceof ICollapsableBlock) {
            ICollapsableBlock collapsableBlock = (ICollapsableBlock)iblockstate.getBlock();
            if(!above) {
                return !BlockSupport.isBeingSupported(worldIn, pos) && collapsableBlock.canCollapse(worldIn, pos);
            }
            return !BlockSupport.isBeingSupported(worldIn, pos);
        }
        return false;
    }

    private boolean supportingFallable(World worldIn, BlockPos pos) {
        IBlockState iblockstate = worldIn.getBlockState(pos.up());
        Block block = iblockstate.getBlock();
        if(block instanceof IFallingBlock || block instanceof BlockFalling) {
            return !BlockSupport.isBeingSupported(worldIn, pos.up());
        }
        return false;
    }

    @Nullable
    @Override
    public Metal getMetal(ItemStack itemStack) {
        return metal;
    }

    @Override
    public int getSmeltAmount(ItemStack itemStack) {
        if (this.isDamageable() && itemStack.isItemDamaged()) {
            double d = (double)(itemStack.getMaxDamage() - itemStack.getItemDamage()) / (double)itemStack.getMaxDamage() - 0.1D;
            return d < 0.0D ? 0 : MathHelper.floor((double)100 * d);
        } else {
            return 100;
        }
    }

    @Override
    public boolean canMelt(ItemStack stack) {
        return true;
    }

    @Nullable
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new ForgeableHeatableHandler(nbt, metal.getSpecificHeat(), metal.getMeltTemp());
    }

}