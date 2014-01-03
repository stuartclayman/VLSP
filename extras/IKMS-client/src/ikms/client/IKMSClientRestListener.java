package ikms.client;

import ikms.data.AbstractRestConsole;

public class IKMSClientRestListener extends AbstractRestConsole {

	private EntityInterface Entity;

	public IKMSClientRestListener(EntityInterface Entity_, int port) {
		Entity = Entity_;
		initialise(port);
	}

	public EntityInterface getEntity() {
		return Entity;
	}

	public void registerCommands() {

		// setup  /update/ handler to handle IR callbacks from the IKMS
		defineRequestHandler("/update/.*", new IKMSClientUpdateHandler());
	}

}