package atlas.global.field;

import info.magnolia.amazon.s3.dam.AmazonS3AssetProvider;
import info.magnolia.amazon.s3.dam.AmazonS3ClientService;
import info.magnolia.cms.beans.config.ServerConfiguration;
import info.magnolia.i18nsystem.SimpleTranslator;
import info.magnolia.ui.api.context.UiContext;
import info.magnolia.ui.api.i18n.I18NAuthoringSupport;
import info.magnolia.ui.form.field.definition.SelectFieldOptionDefinition;
import info.magnolia.ui.form.field.factory.SelectFieldFactory;
import info.magnolia.ui.framework.message.MessagesManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import com.vaadin.v7.data.Item;

public class AmazonS3BucketNameSelectFieldFactory extends SelectFieldFactory<AmazonS3BucketNameSelectFieldDefinition> {
    private AmazonS3ClientService amazonS3ClientService;
    private AmazonS3AssetProvider amazonS3AssetProvider;
    private SimpleTranslator i18n;
    private MessagesManager messagesManager;
    private ServerConfiguration serverConfiguration;

    private List<String> bucketNames = new ArrayList<>();

    @Inject
    public AmazonS3BucketNameSelectFieldFactory(AmazonS3BucketNameSelectFieldDefinition definition, Item relatedFieldItem,
                                                UiContext uiContext, I18NAuthoringSupport i18nAuthoringSupport,
                                                SimpleTranslator i18n, AmazonS3ClientService amazonS3ClientService,
                                                MessagesManager messagesManager, ServerConfiguration serverConfiguration) {
        super(definition, relatedFieldItem, uiContext, i18nAuthoringSupport);

        this.amazonS3ClientService = amazonS3ClientService;
        this.i18n = i18n;
        this.messagesManager = messagesManager;
        this.serverConfiguration = serverConfiguration;
        this.amazonS3AssetProvider = new AmazonS3AssetProvider(amazonS3ClientService, messagesManager, i18n, serverConfiguration);

        getBucketNames();
    }

    public void getBucketNames() {
        Iterator<info.magnolia.dam.api.Item> iterator = amazonS3AssetProvider.getRootFolder().getChildren();
        while (iterator.hasNext()) {
            info.magnolia.dam.api.Item item = iterator.next();

            bucketNames.add(item.getName());
        }
    }

    @Override
    public List<SelectFieldOptionDefinition> getSelectFieldOptionDefinition() {
        List<SelectFieldOptionDefinition> selectFieldOptions = new ArrayList<>();

        for (String bucketName : bucketNames) {
            SelectFieldOptionDefinition selectFieldOptionDefinition = new SelectFieldOptionDefinition();

            selectFieldOptionDefinition.setLabel(bucketName);
            selectFieldOptionDefinition.setValue(bucketName);
            selectFieldOptionDefinition.setName(bucketName);
            selectFieldOptions.add(selectFieldOptionDefinition);
        }

        SelectFieldOptionDefinition selectFieldOptionDefinition = new SelectFieldOptionDefinition();
        selectFieldOptionDefinition.setLabel("optional");
        selectFieldOptionDefinition.setValue("optional");
        selectFieldOptionDefinition.setName("optional");
        selectFieldOptionDefinition.setSelected(true);
        selectFieldOptions.add(selectFieldOptionDefinition);

        return selectFieldOptions;
    }
}