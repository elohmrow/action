package atlas.global.assets.s3;

import info.magnolia.amazon.s3.dam.AmazonS3Asset;
import info.magnolia.amazon.s3.dam.AmazonS3AssetProvider;
import info.magnolia.amazon.s3.dam.AmazonS3ClientService;
import info.magnolia.amazon.s3.dam.AmazonS3Folder;
import info.magnolia.amazon.s3.dam.AmazonS3Item;
import info.magnolia.amazon.s3.util.AmazonS3Utils;
import info.magnolia.cms.beans.config.ServerConfiguration;
import info.magnolia.dam.api.ItemKey;
import info.magnolia.i18nsystem.SimpleTranslator;
import info.magnolia.ui.form.field.upload.UploadReceiver;
import info.magnolia.ui.framework.message.MessagesManager;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class AmazonItemS3AssetProvider extends AmazonS3AssetProvider {
    private AmazonS3Folder rootFolder;
    private ServerConfiguration serverConfiguration;
    private AmazonS3ClientService amazonS3ClientService;

    @Inject
    public AmazonItemS3AssetProvider(AmazonS3ClientService amazonS3ClientService, MessagesManager messagesManager, SimpleTranslator i18n, ServerConfiguration serverConfiguration) {
        super(amazonS3ClientService, messagesManager, i18n, serverConfiguration);
    }

    // override the methods that return [info.magnolia.dam.api.Item]s
    // make them instead return         [info.magnolia.amazon.s3.dam.AmazonS3Item]s
    // override the methods that return [info.magnolia.dam.api.Asset]s
    // make them instead return         [info.magnolia.amazon.s3.dam.AmazonS3Asset]s
    @Override
    public AmazonS3Item getItem(ItemKey itemKey) {
        if (getRootFolder().getItemKey().equals(itemKey)) {
            return getRootFolder();
        }
        if (StringUtils.endsWith(itemKey.getAssetId(), PATH_SEPARATOR)) {
            return getFolder(itemKey);
        } else {
            return getAsset(itemKey);
        }
    }

    @Override
    public AmazonS3Item getItem(String path) {
        if (getRootFolder().getPath().equals(path)) {
            return getRootFolder();
        }
        if (StringUtils.endsWith(path, PATH_SEPARATOR)) {
            return getFolder(path);
        } else {
            return getAsset(path);
        }
    }

    @Override
    public AmazonS3Folder getFolder(String folderPath) {
        if (getRootFolder().getPath().equals(folderPath)) {
            return getRootFolder();
        }
        String bucketName = AmazonS3Utils.getBucketNameFromPath(folderPath);
        String objectKey = AmazonS3Utils.getObjectKeyFromPath(folderPath);
        return getFolder(bucketName, objectKey);
    }

    @Override
    public AmazonS3Asset getAsset(ItemKey itemKey) {
        checkItemKey(itemKey);
        String bucketName = AmazonS3Utils.getBucketNameFromPath(itemKey.getAssetId());
        String key = AmazonS3Utils.getObjectKeyFromPath(itemKey.getAssetId());
        return getAsset(bucketName, key);
    }

    protected AmazonS3Asset getAsset(String bucketName, String key) {
        return (AmazonS3Asset) itemCache.getUnchecked(createItemKeyForItem(bucketName, key));
    }

    @Override
    public AmazonS3Asset getAsset(String assetPath) {
        String bucketName = AmazonS3Utils.getBucketNameFromPath(assetPath);
        String objectKey = AmazonS3Utils.getObjectKeyFromPath(assetPath);
        return getAsset(bucketName, objectKey);
    }

    @Override
    public AmazonS3Folder getRootFolder() {
        if (rootFolder == null) {
            rootFolder = new AmazonS3Folder(this, ItemKey.from(ASSET_PROVIDER_ID + ":" + PATH_SEPARATOR));
        }
        return rootFolder;
    }

    @Override
    public AmazonS3Folder getFolder(ItemKey itemKey) {
        checkItemKey(itemKey);
        if (getRootFolder().getItemKey().equals(itemKey)) {
            return getRootFolder();
        }
        String bucketName = AmazonS3Utils.getBucketNameFromPath(itemKey.getAssetId());
        String key = AmazonS3Utils.getObjectKeyFromPath(itemKey.getAssetId());
        return getFolder(bucketName, key);
    }

    private void checkItemKey(ItemKey itemKey) {
        if (!itemKey.getProviderId().equals(ASSET_PROVIDER_ID)) {
            throw new IllegalItemKeyException(itemKey, this, "Invalid provider id.");
        }
    }

    protected AmazonS3Folder getFolder(String bucketName, String key) {
        return (AmazonS3Folder) itemCache.getUnchecked(createItemKeyForItem(bucketName, key));
    }

    private ItemKey createItemKeyForItem(String bucketName, String key) {
        if (StringUtils.isBlank(key)) {
            return ItemKey.from(ASSET_PROVIDER_ID + ":" + PATH_SEPARATOR + bucketName + PATH_SEPARATOR);
        } else {
            return ItemKey.from(ASSET_PROVIDER_ID + ":" + PATH_SEPARATOR + bucketName + PATH_SEPARATOR + key);
        }
    }

    private LoadingCache<ItemKey, AmazonS3Item> itemCache = CacheBuilder.newBuilder().build(new CacheLoader<ItemKey, AmazonS3Item>() {
        @Override
        public AmazonS3Item load(ItemKey itemKey) {
            String bucketName = AmazonS3Utils.getBucketNameFromPath(itemKey.getAssetId());
            String key = AmazonS3Utils.getObjectKeyFromPath(itemKey.getAssetId());
            AmazonS3Client client = getClient(bucketName);

            if (StringUtils.isBlank(key)) {
                if (client.doesBucketExist(bucketName)) {
                    return new AmazonS3Asset(AmazonItemS3AssetProvider.this, createItemKeyForItem(bucketName, key), serverConfiguration);

                }
            } else if (StringUtils.endsWith(key, PATH_SEPARATOR)) {
                if (client.doesObjectExist(bucketName, key)) {
                    return new AmazonS3Folder(AmazonItemS3AssetProvider.this, createItemKeyForItem(bucketName, key));
                }
            } else {
                if (client.doesObjectExist(bucketName, key)) {
                    return new AmazonS3Asset(AmazonItemS3AssetProvider.this, createItemKeyForItem(bucketName, key), serverConfiguration);
                }
            }
            throw new AssetNotFoundException(itemKey);
        }
    });

    public AmazonS3Asset uploadAsset(AmazonS3Item parent, UploadReceiver uploadReceiver, CannedAccessControlList cannedAcl) {
        String bucketName = AmazonS3Utils.getBucketNameFromPath(parent.getPath());
        String key = AmazonS3Utils.getObjectKeyFromPath(parent.getPath()) + uploadReceiver.getFileName();
        AmazonS3Client client = getClient(bucketName);

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(uploadReceiver.getFileSize());
        client.putObject(new PutObjectRequest(bucketName, key, uploadReceiver.getContentAsStream(), objectMetadata).withCannedAcl(cannedAcl));
        AmazonS3Asset newAsset = getAsset(bucketName, key);
        return newAsset;
    }
}
