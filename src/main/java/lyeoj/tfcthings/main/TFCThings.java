package lyeoj.tfcthings.main;

import lyeoj.tfcthings.init.TFCThingsEntities;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lyeoj.tfcthings.proxy.CommonProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid=TFCThings.MODID, name=TFCThings.NAME, version=TFCThings.VERSION)
public class TFCThings {
	
	public static final String MODID = "tfcthings";
	public static final String NAME = "TerraFirmaThings";
	public static final String VERSION = "0.1.0";
	public static final String CLIENT_PROXY = "lyeoj.tfcthings.proxy.ClientProxy";
	public static final String COMMON_PROXY = "lyeoj.tfcthings.proxy.CommonProxy";
	
	@SidedProxy(clientSide = CLIENT_PROXY, serverSide = COMMON_PROXY)
	public static CommonProxy proxy;
	
	@Mod.Instance
	public static TFCThings instance;
	
	public static final Logger LOGGER = LogManager.getLogger(MODID);
	
	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		LOGGER.info("TFC Things: Starting Pre-Init...");
		for(TFCThingsEntities.NonMobEntityInfo info : TFCThingsEntities.NON_MOB_ENTITY_INFOS) {
			EntityRegistry.registerModEntity(new ResourceLocation(TFCThings.MODID, info.name),
					info.entityClass, info.name, info.id, TFCThings.instance, info.trackingRange,
					info.updateFrequency, info.sendsVelocityUpdates);
		}
		proxy.preInit(event);
	}
	
	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		LOGGER.info("TFC Things: Starting Init...");
		proxy.init(event);
	}
	
	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		LOGGER.info("TFC Things: Starting Post-Init...");
		proxy.postInit(event);
	}	

}
