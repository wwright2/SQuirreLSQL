package net.sourceforge.squirrel_sql.client.session.objectstree;
/*
 * Copyright (C) 2001 Colin Bell
 * colbell@users.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.squirrel_sql.fw.datasetviewer.DataSetViewer;
import net.sourceforge.squirrel_sql.fw.datasetviewer.DataSetException;
import net.sourceforge.squirrel_sql.fw.datasetviewer.DataSetViewerTablePanel;
import net.sourceforge.squirrel_sql.fw.datasetviewer.IDataSet;
import net.sourceforge.squirrel_sql.fw.datasetviewer.IDataSetViewerDestination;
import net.sourceforge.squirrel_sql.fw.datasetviewer.ResultSetDataSet;
import net.sourceforge.squirrel_sql.fw.sql.BaseSQLException;
import net.sourceforge.squirrel_sql.fw.sql.SQLConnection;
import net.sourceforge.squirrel_sql.fw.util.IMessageHandler;

import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.objectstree.databasepanel.DataTypesTab;
import net.sourceforge.squirrel_sql.client.session.objectstree.databasepanel.IDatabasePanelTab;
import net.sourceforge.squirrel_sql.client.session.objectstree.databasepanel.MetaDataTab;
import net.sourceforge.squirrel_sql.client.session.properties.SessionProperties;
import net.sourceforge.squirrel_sql.fw.util.Logger;

public class DatabasePanel extends JTabbedPane {
	/**
	 * This interface defines locale specific strings. This should be
	 * replaced with a property file.
	 */
	//private interface i18n {
		//String META_TAB_TITLE = "Metadata";
		//String META_TAB_DESC = "Show database metadata";
		//String TYPE_TAB_TITLE = "Data Types";
		//String TYPE_TAB_DESC = "Show all the data types available in DBMS";
	//}

	/** Current session. */
	private ISession _session;

	//private MyBaseViewer[] _viewers = new MyBaseViewer[2];

	/** Viewer that displays the Database Metadata. */
	//private MyBaseViewer _metaDataViewer = new MetaDataViewer();

	/** Viewer that displays the Data Types available in DBMS. */
	//private MyBaseViewer _dataTypesViewer = new DataTypesViewer();
	
	/**
	 * Collection of <TT>IDatabasePanelTab</TT> objects displayed in
	 * this tabbed panel.
	 */
	private List _tabs = new ArrayList();

	/** Listens to changes in <CODE>_props</CODE>. */
	private MyPropertiesListener _propsListener;

	/**
	 * ctor specifying the properties for this window.
	 */
	public DatabasePanel(ISession session) {
		super();
		_session = session;
		createUserInterface();
		propertiesHaveChanged(null);
		// Refresh the currently selected tab.
		((IDatabasePanelTab)_tabs.get(getSelectedIndex())).select();
	}

	/**
	 * Add a tab to this panel. If a tab with this title already exists it is
	 * removed from the tabbed pane and the passed tab inserted in its
	 * place. New tabs are inserted at the end.
	 *
	 * @param   tab	 The tab to be added.
	 *
	 * @throws  IllegalArgumentException
	 *		  Thrown if a <TT>null</TT> <TT>ITablePanelTab</TT> passed.
	 */
	public void addDatabasePanelTab(IDatabasePanelTab tab) throws IllegalArgumentException {
		if (tab == null) {
			throw new IllegalArgumentException("Null ITablePanelTab passed");
		}
		tab.setSession(_session);
		//tab.setTableInfo(_ti);
		final String title = tab.getTitle();
		int idx = indexOfTab(title);
		if (idx != -1) {
			removeTabAt(idx);
			_tabs.set(idx, tab);
		} else {
			idx = getTabCount();
			_tabs.add(tab);
		}
		insertTab(title, null, tab.getComponent(), tab.getHint(), idx);
	}

	private void propertiesHaveChanged(String propName) {
		if (propName == null ||
				propName.equals(SessionProperties.IPropertyNames.META_DATA_OUTPUT_CLASS_NAME)) {
//			addMetaDataTab();
//			_metaDataViewer.setHasBeenBuilt(false);
//			_metaDataViewer.load(_session.getSQLConnection());
			addDatabasePanelTab(new MetaDataTab());
		}
		if (propName == null ||
				propName.equals(SessionProperties.IPropertyNames.DATA_TYPES_OUTPUT_CLASS_NAME)) {
//			addDataTypesTab();
//			_dataTypesViewer.setHasBeenBuilt(false);
//			_dataTypesViewer.load(_session.getSQLConnection());
			addDatabasePanelTab(new DataTypesTab());
		}
	}

	private void createUserInterface() {
		addDatabasePanelTab(new MetaDataTab());
		addDatabasePanelTab(new DataTypesTab());

		//addMetaDataTab();
		//addDataTypesTab();
		//_viewers[0] = _metaDataViewer;
		//_viewers[1] = _dataTypesViewer;

		_propsListener = new MyPropertiesListener();
		_session.getProperties().addPropertyChangeListener(_propsListener);

		addChangeListener(new TabbedPaneListener());
	}
/*
	private void addMetaDataTab() {
		addResultSetViewerTab(i18n.META_TAB_TITLE, i18n.META_TAB_DESC, _metaDataViewer,
								_session.getProperties().getMetaDataOutputClassName());
	}
*/
/*
	private void addDataTypesTab() {
		addResultSetViewerTab(i18n.TYPE_TAB_TITLE, i18n.TYPE_TAB_DESC, _dataTypesViewer,
								_session.getProperties().getDataTypesOutputClassName());
	}
*/
	private void addResultSetViewerTab(String title, String description,
												MyBaseViewer viewer,
												String destClassName) {
/*
		IDataSetViewerDestination dest = null;
		try {
			Class destClass = Class.forName(destClassName);
			if (IDataSetViewerDestination.class.isAssignableFrom(destClass) &&
					Component.class.isAssignableFrom(destClass)) {
				dest = (IDataSetViewerDestination)destClass.newInstance();
			}

		} catch (Exception ignore) {
			_session.getApplication().getLogger().showMessage(Logger.ILogTypes.ERROR, ignore.getMessage());
		}
		if (dest == null) {
			dest = new DataSetViewerTablePanel();
		}
		viewer.setDestination(destClassName);
*/
		try {
			viewer.setDestination(destClassName);
		} catch (Exception ignore) {
			_session.getApplication().getLogger().showMessage(Logger.ILogTypes.ERROR, ignore.getMessage());
			viewer.setDestination(new DataSetViewerTablePanel());
		}
		Component comp = viewer.getDestinationComponent();
		int idx = indexOfTab(title);
		if (idx != -1) {
			removeTabAt(idx);
			insertTab(title, null, new JScrollPane(comp), description, idx);
		} else {
			addTab(title, null, new JScrollPane(comp), description);
		}
	}

	private abstract class MyBaseViewer extends DataSetViewer {
		private boolean _hasBeenBuilt = false;

		abstract void loadImpl(SQLConnection conn) throws DataSetException,
										BaseSQLException, SQLException;

		void load(SQLConnection conn) {
			if (!_hasBeenBuilt) {
				try {
					clearDestination();
					loadImpl(conn);
				} catch (Exception ex) {
					IMessageHandler msgHandler = _session.getMessageHandler();
					msgHandler.showMessage("Error in: " + getClass().getName());
					msgHandler.showMessage(ex);
				}
				_hasBeenBuilt = true;
			}
		}

		public void setHasBeenBuilt(boolean value) {
			_hasBeenBuilt = value;
		}
	}
/*
	private class DataTypesViewer extends MyBaseViewer {
		void loadImpl(SQLConnection conn) throws DataSetException, BaseSQLException, SQLException {
			if (conn != null) {
				show(new ResultSetDataSet(conn.getTypeInfo()), _session.getMessageHandler());
			}
		}
	}

	private class MetaDataViewer extends MyBaseViewer {
		void loadImpl(SQLConnection conn) throws DataSetException, BaseSQLException, SQLException {
			if (conn != null) {
				show(conn.createMetaDataDataSet(_session.getMessageHandler()), _session.getMessageHandler());
			}
		}
	}
*/
	private class MyPropertiesListener implements PropertyChangeListener {
		public void propertyChange(PropertyChangeEvent evt) {
			DatabasePanel.this.propertiesHaveChanged(evt.getPropertyName());
		}
	}

	private class TabbedPaneListener implements ChangeListener {
		public void stateChanged(ChangeEvent evt) {
			Object src = evt.getSource();
			if (src instanceof JTabbedPane) {
				int idx = ((JTabbedPane)src).getSelectedIndex();
				if (idx != -1) {
//					DatabasePanel.this._viewers[idx].load(DatabasePanel.this._session.getSQLConnection());
					((IDatabasePanelTab)_tabs.get(getSelectedIndex())).select();
				}
			}
		}
	}
}
