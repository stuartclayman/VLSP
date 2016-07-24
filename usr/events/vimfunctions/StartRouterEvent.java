package usr.events.vimfunctions;

import usr.engine.EventEngine;
import usr.events.EventDelegate;
import usr.events.AbstractExecutableEvent;
import usr.events.vim.StartRouter;
import usr.logging.Logger;
import usr.logging.USR;
import usr.vim.VimFunctions;
import us.monoid.json.JSONObject;
import us.monoid.json.JSONException;

/** Class represents a global controller event */
public class StartRouterEvent extends AbstractExecutableEvent implements
		StartRouter {
	public final String address;
	public final String name;
	public final String parameters;

	public StartRouterEvent(long time, EventEngine eng) {
		super(time, eng);
		this.name = null;
		this.address = null;
		this.parameters = null;
	}

	public StartRouterEvent(long time, EventEngine eng, String address,
			String name, String parameters) {
		super(time, eng, parameters);
		this.name = name;
		this.address = address;
		this.parameters = parameters;
	}

	/**
	 * Create a StartRouterEvent from an existing generic StartRouterEvent.
	 */
	public StartRouterEvent(usr.events.vim.StartRouterEvent sre) {
		this(sre.time, sre.engine, sre.address, sre.name, sre.parameters); // added
																			// parameters
																			// here
																			// (Lefteris)
	}

	/**
	 * Execute the event, pass in a context object, and return a JSON object
	 * with information
	 */
	@Override
	public JSONObject eventBody(EventDelegate obj) {
		Object context = getContextObject();

		if (context instanceof VimFunctions) {
			try {
				VimFunctions vim = (VimFunctions) context;

				if (address == null) {
					if (parameters != null) { // added option for parameters
						return vim.createRouter(parameters);
					} else {
						return vim.createRouter();
					}
				} else {
					if (parameters == null) { // added option for parameters

						return vim.createRouter(name, address);

					} else {

						return vim.createRouter(name, address, parameters);
					}
				}
			} catch (JSONException jse) {
				return fail("JSONException " + jse.getMessage());
			}

		} else {
			return fail("Context object is not a VimFunctions");
		}
	}

	@Override
	public String toString() {
		String str;

		str = "StartRouter: " + time + " " + getName();
		return str;
	}

	private String getName() {
		String str = "";

		if (name != null) {
			str += " " + name;
		}

		if (address != null) {
			str += " " + address;
		}

		return str;
	}

}
