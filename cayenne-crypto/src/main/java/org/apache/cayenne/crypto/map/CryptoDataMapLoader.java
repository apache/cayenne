package org.apache.cayenne.crypto.map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.resource.Resource;

/**
 * DataMapLoader is overridden to warn and disable optimistic locking
 * for encrypted attributes since they don't support optimistic locking.
 * The encryption method will return different encrypted values every
 * time it is executed, even with the same plain text, so these values 
 * cannot be compared for locking purposes.
 * 
 * @since 4.2
 *
 */
public class CryptoDataMapLoader implements DataMapLoader {

	protected final DataMapLoader delegate;
	protected final ColumnMapper columnMapper;
	protected final JdbcEventLogger jdbcEventLogger;
		
	public CryptoDataMapLoader(
			@Inject DataMapLoader delegate, 
			@Inject ColumnMapper columnMapper, 
			@Inject JdbcEventLogger jdbcEventLogger) {
		
		this.delegate = delegate;
		this.columnMapper = columnMapper;
		this.jdbcEventLogger = jdbcEventLogger;
	}
	
	@Override
	public DataMap load(Resource configurationResource) throws CayenneRuntimeException {
		DataMap result = delegate.load(configurationResource);
		
		for (ObjEntity entity : result.getObjEntities()) {
			if (entity.getLockType() == ObjEntity.LOCK_TYPE_OPTIMISTIC) {
				for (ObjAttribute attr : entity.getAttributes()) {
					if (attr.isUsedForLocking() && 
						attr.getDbAttribute() != null && 
						columnMapper.isEncrypted(attr.getDbAttribute())) {
						
						String attrName = entity.getName() + "." + attr.getName();
						jdbcEventLogger.log("WARN: Encrypted attributes like '" + attrName + "' cannot be used for " +
								"optimistic locking. Locking will be disabled for this attribute.");
						
						attr.setUsedForLocking(false);
					}
				}
			}
		}
		
		return result;
	}

}
