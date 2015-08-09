package org.jboss.forge.addon.springboot.descriptors;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;

import org.jboss.shrinkwrap.descriptor.api.DescriptorExportException;
import org.jboss.shrinkwrap.descriptor.api.persistence.PersistenceUnitCommon;

public class SpringBootPropertiesDescriptor implements SpringBootDescriptor {

	private static final String MESSAGE = "Not a real persistence descriptor";
	private Properties properties;

	public SpringBootPropertiesDescriptor(Properties properties) {
		this.properties = properties;
	}

	@Override
	public String getDescriptorName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String exportAsString() throws DescriptorExportException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void exportTo(OutputStream output) throws DescriptorExportException,
			IllegalArgumentException {
		try {
			properties.store(output, null);
		} catch (IOException e) {
			throw new DescriptorExportException("Failed to store descriptor", e);
		}
	}

	@Override
	public PersistenceUnitCommon createPersistenceUnit() {
		throw new UnsupportedOperationException(MESSAGE);
	}

	@Override
	public List getAllPersistenceUnit() {
		throw new UnsupportedOperationException(MESSAGE);
	}

	@Override
	public PersistenceUnitCommon getOrCreatePersistenceUnit() {
		throw new UnsupportedOperationException(MESSAGE);
	}

	@Override
	public String getVersion() {
		throw new UnsupportedOperationException(MESSAGE);
	}

	@Override
	public Object removeAllPersistenceUnit() {
		throw new UnsupportedOperationException(MESSAGE);
	}

	@Override
	public Object removeVersion() {
		throw new UnsupportedOperationException(MESSAGE);
	}

	@Override
	public Object version(String arg0) {
		throw new UnsupportedOperationException(MESSAGE);
	}

}
