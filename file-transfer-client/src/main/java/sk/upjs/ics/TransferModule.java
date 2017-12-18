package sk.upjs.ics;

import com.google.inject.AbstractModule;
import sk.upjs.ics.state.FileStateManager;
import sk.upjs.ics.state.StateManager;

/**
 * Created by Tomas on 17.12.2017.
 */
public class TransferModule extends AbstractModule {
    @Override protected void configure() {
        bind(StateManager.class).to(FileStateManager.class);
    }
}
