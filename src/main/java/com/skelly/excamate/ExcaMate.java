package com.skelly.excamate;

import com.skelly.excamate.config.ExcaMateConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcaMate implements ModInitializer {
	public static final String MOD_ID = "excamate";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static ExcaMateConfig config;

	@Override
	public void onInitialize() {
		LOGGER.info("ExcaMate initializing...");
		config = ExcaMateConfig.load();
		ExcaMateNetworking.register();
		ExcaMateDropSweeper.register();
		LOGGER.info(
			"ExcaMate loaded! Auto-pickup: {}, auto-XP: {}, mode limits: vein={}, branch={}, excavation={}, default mode: {}",
			config.autoPickup,
			config.autoCollectXp,
			config.veinMaxBlocks,
			config.branchMaxBlocks,
			config.excaMateMaxBlocks,
			config.defaultMode
		);
	}
}
