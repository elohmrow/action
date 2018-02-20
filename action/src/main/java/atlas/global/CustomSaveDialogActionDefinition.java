package atlas.global;

import info.magnolia.ui.api.action.CommandActionDefinition;

public class CustomSaveDialogActionDefinition extends CommandActionDefinition {
    public CustomSaveDialogActionDefinition() {
        setImplementationClass(CustomSaveDialogAction.class);
    }
}