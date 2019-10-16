package affidavit.eval;

import affidavit.config.Config;
import affidavit.config.InitializationStrategy;

public enum AlgorithmSetting {
	SINGLE_IDs(InitializationStrategy.SINGLE_IDs), BEST_IDs(InitializationStrategy.BEST_ID), FULL_IDs(
			InitializationStrategy.FULL_IDs);
	public final InitializationStrategy INITIALIZATION_STRATEGY;

	AlgorithmSetting(InitializationStrategy initStrat) {
		this.INITIALIZATION_STRATEGY = initStrat;
	}

	public void apply() {
		Config.INITIALIZATION_STRATEGY = INITIALIZATION_STRATEGY;
	}
}
