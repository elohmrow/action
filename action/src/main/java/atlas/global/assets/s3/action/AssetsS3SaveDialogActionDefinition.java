package atlas.global.assets.s3.action;

import info.magnolia.ui.api.action.CommandActionDefinition;

public class AssetsS3SaveDialogActionDefinition extends CommandActionDefinition {
    public AssetsS3SaveDialogActionDefinition() {
        setImplementationClass(AssetsS3SaveDialogAction.class);
    }
}