package usr.events.vimfunctions;

import usr.events.Event;
import usr.events.ExecutableEvent;
import usr.events.EventResolver;

/**
 * An EventResolver for the VimFunctions events.
 * It resolves generic Events from usr.events.vim into
 * ExecutableEvents for usr.events.vimfunctions
 */
public class VFEventResolver implements EventResolver {
    public VFEventResolver() {
    }

    public ExecutableEvent resolveEvent(Event e) {
        if (e instanceof usr.events.vim.StartRouterEvent) {
            usr.events.vim.StartRouterEvent sre = (usr.events.vim.StartRouterEvent)e;
            return new usr.events.vimfunctions.StartRouterEvent(sre);

        } else if (e instanceof   usr.events.vim.EndRouterEvent) {
            usr.events.vim.EndRouterEvent ere = (usr.events.vim.EndRouterEvent)e;
            return new usr.events.vimfunctions.EndRouterEvent(ere);

        } else if (e instanceof   usr.events.vim.StartLinkEvent) {
            usr.events.vim.StartLinkEvent sle = (usr.events.vim.StartLinkEvent)e;
            return new usr.events.vimfunctions.StartLinkEvent(sle);

        } else if (e instanceof   usr.events.vim.EndLinkEvent) {
            usr.events.vim.EndLinkEvent ele = (usr.events.vim.EndLinkEvent)e;
            return new usr.events.vimfunctions.EndLinkEvent(ele);

        } else if (e instanceof  usr.events.vim.StartAppEvent) {
            usr.events.vim.StartAppEvent ase = (usr.events.vim.StartAppEvent)e;
            return new usr.events.vimfunctions.StartAppEvent(ase);

        } else {
        }

        return null;

    }
}
