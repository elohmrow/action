package atlas.global;

import info.magnolia.amazon.s3.dam.AmazonS3AssetProvider;
import info.magnolia.amazon.s3.dam.AmazonS3ClientService;
import info.magnolia.amazon.s3.util.AmazonS3Utils;
import info.magnolia.cms.beans.config.ServerConfiguration;
import info.magnolia.cms.core.Path;
import info.magnolia.commands.CommandsManager;
import info.magnolia.event.EventBus;
import info.magnolia.i18nsystem.SimpleTranslator;
import info.magnolia.jcr.util.NodeUtil;
import info.magnolia.ui.api.action.ActionExecutionException;
import info.magnolia.ui.api.context.UiContext;
import info.magnolia.ui.api.event.AdmincentralEventBus;
import info.magnolia.ui.api.event.ContentChangedEvent;
import info.magnolia.ui.form.EditorCallback;
import info.magnolia.ui.form.field.upload.UploadReceiver;
import info.magnolia.ui.framework.action.AbstractCommandAction;
import info.magnolia.ui.framework.message.MessagesManager;
import info.magnolia.ui.vaadin.integration.jcr.JcrItemAdapter;
import info.magnolia.ui.vaadin.integration.jcr.JcrNodeAdapter;
import info.magnolia.ui.vaadin.integration.jcr.ModelConstants;

import javax.inject.Inject;
import javax.inject.Named;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

public class CustomSaveDialogAction extends AbstractCommandAction<CustomSaveDialogActionDefinition> {
    private JcrItemAdapter item;
    private AmazonS3ClientService amazonS3ClientService;
    private SimpleTranslator i18n;
    private MessagesManager messagesManager;
    private ServerConfiguration serverConfiguration;
    private EditorCallback callback;
    private EventBus eventBus;

    @Inject
    public CustomSaveDialogAction(CustomSaveDialogActionDefinition definition, JcrItemAdapter item, CommandsManager commandsManager,
                                  UiContext uiContext, SimpleTranslator i18n, AmazonS3ClientService amazonS3ClientService, MessagesManager messagesManager, ServerConfiguration serverConfiguration, EditorCallback callback, @Named(AdmincentralEventBus.NAME) EventBus eventBus) {
        super(definition, item, commandsManager, uiContext, i18n);
        this.item = item;
        this.amazonS3ClientService = amazonS3ClientService;
        this.i18n = i18n;
        this.messagesManager = messagesManager;
        this.serverConfiguration = serverConfiguration;
        this.callback = callback;
        this.eventBus = eventBus;
    }

    @Override
    public void execute() throws ActionExecutionException {
        UploadReceiver upload = (UploadReceiver) item.getItemProperty("upload").getValue();
        String acl = (String) item.getItemProperty("acl").getValue();
        String bucketName = (String) item.getItemProperty("selectedItem").getValue();
        String key = AmazonS3Utils.getObjectKeyFromPath(AmazonS3Utils.getFullPath(bucketName, null) + upload.getFileName());

        if (!acl.equals("optional") || !bucketName.equals("optional")) {

            AmazonS3AssetProvider amazonS3AssetProvider = new AmazonS3AssetProvider(amazonS3ClientService, messagesManager, i18n, serverConfiguration);

            try {
                CannedAccessControlList cannedAcl = AmazonS3Utils.getCannedAclFromString(acl);
                AmazonS3Client client = amazonS3AssetProvider.getClient(bucketName);
                ObjectMetadata objectMetadata = new ObjectMetadata();
                objectMetadata.setContentLength(upload.getFileSize());
                client.putObject(new PutObjectRequest(bucketName, key, upload.getContentAsStream(), objectMetadata).withCannedAcl(cannedAcl));
            } catch (AmazonClientException e) {
                throw new ActionExecutionException(e);
            }

            eventBus.fireEvent(new ContentChangedEvent(key));
        }

        item.getItemProperty("upload").setValue("");

        final Node node;
        try {
            node = ((JcrNodeAdapter) item).applyChanges();
            setNodeName(node, ((JcrNodeAdapter) item));

            node.addNode("jcr:content", "mgnl:resource").setProperty("jcr:data", node.getSession().getValueFactory().createBinary(upload.getContentAsStream()));
            node.getNode("jcr:content").setProperty("fileName", upload.getFileName());
            node.getNode("jcr:content").setProperty("extension", upload.getExtension());
            node.getNode("jcr:content").setProperty("jcr:mimeType", upload.getMimeType());
            node.getNode("jcr:content").setProperty("size", upload.getFileSize());
            node.getSession().save();
        } catch (RepositoryException e) {
            throw new ActionExecutionException(e);
        }

        callback.onSuccess(getDefinition().getName());
    }

    protected void setNodeName(Node node, JcrNodeAdapter item) throws RepositoryException {
        String propertyName = "name";
        if (node.hasProperty(propertyName) && !node.hasProperty(ModelConstants.JCR_NAME)) {
            Property property = node.getProperty(propertyName);
            String newNodeName = property.getString();
            if (!node.getName().equals(Path.getValidatedLabel(newNodeName))) {
                newNodeName = Path.getUniqueLabel(node.getSession(), node.getParent().getPath(), Path.getValidatedLabel(newNodeName));
                item.setNodeName(newNodeName);
                NodeUtil.renameNode(node, newNodeName);
            }
        }
    }
}