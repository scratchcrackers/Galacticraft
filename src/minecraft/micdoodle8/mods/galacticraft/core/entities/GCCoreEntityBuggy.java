package micdoodle8.mods.galacticraft.core.entities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import micdoodle8.mods.galacticraft.core.items.GCCoreItems;
import micdoodle8.mods.galacticraft.core.network.GCCorePacketControllableEntity;
import micdoodle8.mods.galacticraft.core.network.GCCorePacketEntityUpdate;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class GCCoreEntityBuggy extends GCCoreEntityControllable implements IInventory
{
    public int fuel;
    public int currentDamage;
    public int timeSinceHit;
    public int rockDirection;
    public double speed;
    float maxSpeed = 0.5F;
    float accel = 0.2F;
    float turnFactor = 3.0F;
    public String texture;
    ItemStack[] cargoItems;
	public float turnProgress = 0;
	private final boolean firstPacketSent = false;
	public float rotationYawBuggy;
    public double boatX;
    public double boatY;
    public double boatZ;
    public double boatYaw;
    public double boatPitch;
	public int boatPosRotationIncrements;

    public GCCoreEntityBuggy(World var1)
    {
        super(var1);
        this.setSize(0.98F, 0.7F);
        this.yOffset = 2.5F;
        this.cargoItems = new ItemStack[36];
        this.fuel = 0;
        this.currentDamage = 18;
        this.timeSinceHit = 19;
        this.rockDirection = 20;
        this.speed = 0.0D;
        this.preventEntitySpawning = true;
        this.dataWatcher.addObject(this.currentDamage, new Integer(0));
        this.dataWatcher.addObject(this.timeSinceHit, new Integer(0));
        this.dataWatcher.addObject(this.rockDirection, new Integer(1));
        this.ignoreFrustumCheck = true;
    }

    public GCCoreEntityBuggy(World var1, double var2, double var4, double var6)
    {
        this(var1);
        this.setPosition(var2, var4 + this.yOffset, var6);
    }

    public ModelBase getModel()
    {
        return null;
    }

    @Override
	protected void entityInit() {}

    @Override
	protected boolean canTriggerWalking()
    {
        return false;
    }
    
    @Override
	public AxisAlignedBB getBoundingBox()
    {
        return this.boundingBox;
    }

    @Override
	public boolean canBePushed()
    {
        return false;
    }

    @Override
	public double getMountedYOffset()
    {
        return this.height - 3.0D;
    }

    @Override
	public boolean canBeCollidedWith()
    {
        return !this.isDead;
    }

    @Override
	public void updateRiderPosition()
    {
        if (this.riddenByEntity != null)
        {
            final double var1 = Math.cos(this.rotationYaw * Math.PI / 180.0D + 114.8) * -0.5D;
            final double var3 = Math.sin(this.rotationYaw * Math.PI / 180.0D + 114.8) * -0.5D;
            this.riddenByEntity.setPosition(this.posX + var1, this.posY - 2 + this.riddenByEntity.getYOffset(), this.posZ + var3);
        }
    }
	
	public void setPositionRotationAndMotion(double x, double y, double z, float yaw, float pitch, double motX, double motY, double motZ)
	{
		if(worldObj.isRemote)
		{
	        boatX = x;
	        boatY = y;
	        boatZ = z;
	        boatYaw = yaw;
	        boatPitch = pitch;
	        motionX = motX;
	        motionY = motY;
	        motionZ = motZ;
	        boatPosRotationIncrements = 5;
		}
		else
		{
			setPosition(x, y, z);
			setRotation(yaw, pitch);
			motionX = motX;
			motionY = motY;
			motionZ = motZ;
		}
	}

    @Override
	public void performHurtAnimation()
    {
        this.dataWatcher.updateObject(this.rockDirection, Integer.valueOf(-this.dataWatcher.getWatchableObjectInt(this.rockDirection)));
        this.dataWatcher.updateObject(this.timeSinceHit, Integer.valueOf(10));
        this.dataWatcher.updateObject(this.currentDamage, Integer.valueOf(this.dataWatcher.getWatchableObjectInt(this.currentDamage) * 5));
    }

    @Override
	public boolean attackEntityFrom(DamageSource var1, int var2)
    {
        if (this.isDead)
        {
            return true;
        }
        else
        {
            this.dataWatcher.updateObject(this.rockDirection, Integer.valueOf(-this.dataWatcher.getWatchableObjectInt(this.rockDirection)));
            this.dataWatcher.updateObject(this.timeSinceHit, Integer.valueOf(10));
            this.dataWatcher.updateObject(this.currentDamage, Integer.valueOf(this.dataWatcher.getWatchableObjectInt(this.currentDamage) + var2 * 10));
            this.setBeenAttacked();

            if (var1.getEntity() instanceof EntityPlayer && ((EntityPlayer)var1.getEntity()).capabilities.isCreativeMode)
            {
                this.dataWatcher.updateObject(this.currentDamage, 100);
            }

            if (this.dataWatcher.getWatchableObjectInt(this.currentDamage) > 2)
            {
                if (this.riddenByEntity != null)
                {
                    this.riddenByEntity.mountEntity(this);
                }

                if (!this.worldObj.isRemote)
                {
                    if (this.riddenByEntity != null)
                    {
                        this.riddenByEntity.mountEntity(this);
                    }

                    this.dropBuggyAsItem();
                }

                this.setDead();
            }

            return true;
        }
    }

    public void dropBuggyAsItem()
    {
    	if (this.getItemsDropped() == null)
    	{
    		return;
    	}

        for(final ItemStack item : this.getItemsDropped())
        {
            this.entityDropItem(item, 0);
        }
    }

    public List<ItemStack> getItemsDropped()
    {
        final List<ItemStack> items = new ArrayList<ItemStack>();
        items.add(new ItemStack(GCCoreItems.buggy));
    	return items;
    }

	@Override
    public void setPositionAndRotation2(double d, double d1, double d2, float f, float f1, int i)
    {
		if (this.riddenByEntity != null)
		{
			if(riddenByEntity instanceof EntityPlayer && FMLClientHandler.instance().getClient().thePlayer.equals(this.riddenByEntity))
			{
			}
			else
			{
	            boatPosRotationIncrements = i + 5;
		        boatX = d;
		        boatY = d1 + (this.riddenByEntity == null ? 1 : 0);
		        boatZ = d2;
		        boatYaw = (double)f;
		        boatPitch = (double)f1;
			}
		}
    }

    @Override
	public void onUpdate()
    {
        super.onUpdate();
        
        if(worldObj.isRemote && (riddenByEntity == null || !(riddenByEntity instanceof EntityPlayer) || !FMLClientHandler.instance().getClient().thePlayer.equals(this.riddenByEntity)))
        {
            double x;
            double y;
            double var12;
            double z;
            if (boatPosRotationIncrements > 0)
            {
                x = posX + (boatX - posX) / (double)boatPosRotationIncrements;
                y = posY + (boatY - posY) / (double)boatPosRotationIncrements;
                z = posZ + (boatZ - posZ) / (double)boatPosRotationIncrements;
                var12 = MathHelper.wrapAngleTo180_double(boatYaw - (double)rotationYaw);
                rotationYaw = (float)((double)rotationYaw + var12 / (double)boatPosRotationIncrements);
                rotationPitch = (float)((double)rotationPitch + (boatPitch - (double)rotationPitch) / (double)boatPosRotationIncrements);
                --boatPosRotationIncrements;
                setPosition(x, y, z);
                setRotation(rotationYaw, rotationPitch);     
            }
            else
            {
                x = posX + motionX;
                y = posY + motionY;
                z = posZ + motionZ;
        		if (this.riddenByEntity != null)
                setPosition(x, y, z);

                if (onGround)
                {
                    motionX *= 0.5D;
                    motionY *= 0.5D;
                    motionZ *= 0.5D;
                }

                motionX *= 0.9900000095367432D;
                motionY *= 0.949999988079071D;
                motionZ *= 0.9900000095367432D;
            }
            return;
        }

        if (this.dataWatcher.getWatchableObjectInt(this.timeSinceHit) > 0)
        {
            this.dataWatcher.updateObject(this.timeSinceHit, Integer.valueOf(this.dataWatcher.getWatchableObjectInt(this.timeSinceHit) - 1));
        }

        if (this.dataWatcher.getWatchableObjectInt(this.currentDamage) > 0)
        {
            this.dataWatcher.updateObject(this.currentDamage, Integer.valueOf(this.dataWatcher.getWatchableObjectInt(this.currentDamage) - 1));
        }

        final byte var20 = 5;
        final double var2 = 0.0D;
        int var4;

        for (var4 = 0; var4 < var20; ++var4)
        {
        }

        if (var2 < 1.0D)
        {
            this.motionY -= 0.04D;
        }

        if (this.fuel <= 0)
        {
            for (var4 = 0; var4 < this.getSizeInventory(); ++var4)
            {
                final ItemStack var22 = this.getStackInSlot(var4);

                if (var22 != null && var22.itemID == Item.coal.itemID)
                {
                    this.decrStackSize(var4, 1);
                    this.fuel += 1500;

                    if (this.riddenByEntity != null && this.riddenByEntity instanceof EntityPlayer)
                    {
                        final EntityPlayer var6 = (EntityPlayer)this.riddenByEntity;
                        var6.addChatMessage("Added Fuel");
                        break;
                    }
                }
            }
        }
        
        if (this.riddenByEntity == null)
        {
        	this.yOffset = 5;
        }

        if (this.inWater && this.speed > 0.2D)
        {
            this.worldObj.playSoundEffect((float)this.posX, (float)this.posY, (float)this.posZ, "random.fizz", 0.5F, 2.6F + (this.worldObj.rand.nextFloat() - this.worldObj.rand.nextFloat()) * 0.8F);
        }

        this.speed *= 0.98D;

        if (this.speed > this.maxSpeed)
        {
            this.speed = this.maxSpeed;
        }

        if (this.isCollidedHorizontally)
        {
            this.speed *= 0.9;
            this.motionY = 0.1D;
        }

        this.motionX = -(this.speed * Math.cos((this.rotationYaw - 90F) * Math.PI / 180.0D ));
        this.motionZ = -(this.speed * Math.sin((this.rotationYaw - 90F) * Math.PI / 180.0D ));
        

		if (this.worldObj.isRemote)
		{
			moveEntity(motionX, motionY, motionZ);
		}

        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
		
		if(this.worldObj.isRemote && riddenByEntity instanceof EntityPlayer && FMLClientHandler.instance().getClient().thePlayer.equals(this.riddenByEntity))
		{
			PacketDispatcher.sendPacketToServer(GCCorePacketEntityUpdate.buildUpdatePacket(this));
		}
		
		if(!worldObj.isRemote && ticksExisted % 5 == 0)
		{
			PacketDispatcher.sendPacketToAllAround(posX, posY, posZ, 50, dimension, GCCorePacketEntityUpdate.buildUpdatePacket(this));
		}
    }

    @Override
	protected void readEntityFromNBT(NBTTagCompound var1)
    {
        this.fuel = var1.getInteger("Fuel");
        final NBTTagList var2 = var1.getTagList("Items");
        this.cargoItems = new ItemStack[this.getSizeInventory()];

        for (int var3 = 0; var3 < var2.tagCount(); ++var3)
        {
            final NBTTagCompound var4 = (NBTTagCompound)var2.tagAt(var3);
            final int var5 = var4.getByte("Slot") & 255;

            if (var5 >= 0 && var5 < this.cargoItems.length)
            {
                this.cargoItems[var5] = ItemStack.loadItemStackFromNBT(var4);
            }
        }
    }

    @Override
	protected void writeEntityToNBT(NBTTagCompound var1)
    {
        var1.setInteger("fuel", this.fuel);
        final NBTTagList var2 = new NBTTagList();

        for (int var3 = 0; var3 < this.cargoItems.length; ++var3)
        {
            if (this.cargoItems[var3] != null)
            {
                final NBTTagCompound var4 = new NBTTagCompound();
                var4.setByte("Slot", (byte)var3);
                this.cargoItems[var3].writeToNBT(var4);
                var2.appendTag(var4);
            }
        }

        var1.setTag("Items", var2);
    }

    @Override
	public int getSizeInventory()
    {
        return 27;
    }

    @Override
	public ItemStack getStackInSlot(int var1)
    {
        return this.cargoItems[var1];
    }

    @Override
	public ItemStack decrStackSize(int var1, int var2)
    {
        if (this.cargoItems[var1] != null)
        {
            ItemStack var3;

            if (this.cargoItems[var1].stackSize <= var2)
            {
                var3 = this.cargoItems[var1];
                this.cargoItems[var1] = null;
                return var3;
            }
            else
            {
                var3 = this.cargoItems[var1].splitStack(var2);

                if (this.cargoItems[var1].stackSize == 0)
                {
                    this.cargoItems[var1] = null;
                }

                return var3;
            }
        }
        else
        {
            return null;
        }
    }

    @Override
	public ItemStack getStackInSlotOnClosing(int var1)
    {
        if (this.cargoItems[var1] != null)
        {
            final ItemStack var2 = this.cargoItems[var1];
            this.cargoItems[var1] = null;
            return var2;
        }
        else
        {
            return null;
        }
    }

    @Override
	public void setInventorySlotContents(int var1, ItemStack var2)
    {
        this.cargoItems[var1] = var2;

        if (var2 != null && var2.stackSize > this.getInventoryStackLimit())
        {
            var2.stackSize = this.getInventoryStackLimit();
        }
    }

    @Override
	public String getInvName()
    {
        return "Buggy";
    }

    @Override
	public int getInventoryStackLimit()
    {
        return 64;
    }

    @Override
	public void onInventoryChanged() {}

    @Override
	public boolean isUseableByPlayer(EntityPlayer var1)
    {
        return true;
    }

    @Override
	public void openChest() {}

    @Override
	public void closeChest() {}

    @Override
	public boolean interact(EntityPlayer var1)
    {
        var1.inventory.getCurrentItem();

        if (this.worldObj.isRemote)
        {
            return true;
        }
        else if (this.riddenByEntity != null && this.riddenByEntity instanceof EntityPlayer && this.riddenByEntity != var1)
        {
            return true;
        }
        else
        {
            var1.mountEntity(this);
            return true;
        }
    }

	@Override
	public boolean pressKey(int key)
	{
    	if(worldObj.isRemote && (key == 6 || key == 8 || key == 9))
    	{
    		PacketDispatcher.sendPacketToServer(GCCorePacketControllableEntity.buildKeyPacket(key));
    		return true;
    	}
		switch(key)
		{
			case 0 : //Accelerate
			{
				this.speed += this.accel / 20D;
				return true;
			}
			case 1 : //Deccelerate
			{
				this.speed -= this.accel / 20D;
				return true;
			}
			case 2 : //Left
			{
				this.rotationYaw -= 0.5F * this.turnFactor;
				return true;
			}
			case 3 : //Right
			{
				this.rotationYaw += 0.5F * this.turnFactor;
				return true;
			}
		}
		
		return false;
	}

	@Override
	public boolean func_94042_c() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean func_94041_b(int i, ItemStack itemstack) {
		// TODO Auto-generated method stub
		return false;
	}
}