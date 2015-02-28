package infra;

import jason.infra.centralised.RunCentralisedMAS;

public class NoisyGoldMinersRunner extends RunCentralisedMAS {

    @Override
	public int init(java.lang.String[] args) {
		System.out.println("\n\n\n\nMy runner\n\n\n\n\n");
		return super.init(args);
	}
}
