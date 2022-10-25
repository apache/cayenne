/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.dba.sqlserver;

import java.util.Arrays;
import java.util.List;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.SQLTreeProcessor;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.sybase.SybaseAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.resource.ResourceLocator;

/**
 * <p>
 * Cayenne DbAdapter implementation for <a
 * href="http://www.microsoft.com/sql/">Microsoft SQL Server </a> engine.
 * </p>
 * <h3>Microsoft Driver Settings</h3>
 * <p>
 * Sample connection settings to use with MS SQL Server are shown below:
 *
 * <pre>
 *       sqlserver.jdbc.username = test
 *       sqlserver.jdbc.password = secret
 *       sqlserver.jdbc.url = jdbc:sqlserver://192.168.0.65;databaseName=cayenne;SelectMethod=cursor
 *       sqlserver.jdbc.driver = com.microsoft.sqlserver.jdbc.SQLServerDriver
 * </pre>
 * <p>
 * <i>Note on case-sensitive LIKE: if your application requires case-sensitive
 * LIKE support, ask your DBA to configure the database to use a case-senstitive
 * collation (one with "CS" in symbolic collation name instead of "CI", e.g.
 * "SQL_Latin1_general_CP1_CS_AS"). </i>
 * </p>
 * <h3>jTDS Driver Settings</h3>
 * <p>
 * jTDS is an open source driver that can be downloaded from <a href=
 * "http://jtds.sourceforge.net">http://jtds.sourceforge.net </a>. It supports
 * both SQLServer and Sybase. Sample SQLServer settings are the following:
 * </p>
 *
 * <pre>
 *       sqlserver.jdbc.username = test
 *       sqlserver.jdbc.password = secret
 *       sqlserver.jdbc.url = jdbc:jtds:sqlserver://192.168.0.65/cayenne
 *       sqlserver.jdbc.driver = net.sourceforge.jtds.jdbc.Driver
 * </pre>
 *
 * @since 1.1
 */
public class SQLServerAdapter extends SybaseAdapter {

	/**
	 * Stores the major version of the database.
	 * Database versions 12 and higher supports the use of LIMIT,lower versions use TOP N.
	 *
	 * @since 4.2
	 */
	private Integer version;

	private final List<String> SYSTEM_SCHEMAS = Arrays.asList(
			"db_accessadmin", "db_backupoperator",
			"db_datareader", "db_datawriter", "db_ddladmin", "db_denydatareader",
			"db_denydatawriter", "sys", "db_owner", "db_securityadmin", "INFORMATION_SCHEMA"
	);

	private final List<String> SYSTEM_CATALOGS = Arrays.asList(
			// master dbo
			"MSreplication_options", "spt_fallback_db", "spt_fallback_dev", "spt_fallback_usg", "spt_monitor",
			// msdb dbo
			"autoadmin_managed_databases", "autoadmin_master_switch", "autoadmin_system_flags",
			"autoadmin_task_agent_metadata", "autoadmin_task_agents", "backupfile",
			"backupfilegroup", "backupmediafamily", "backupmediaset", "backupset",
			"dm_hadr_automatic_seeding_history", "external_libraries_installed",
			"log_shipping_monitor_alert", "log_shipping_monitor_error_detail",
			"log_shipping_monitor_history_detail", "log_shipping_monitor_primary",
			"log_shipping_monitor_secondary", "log_shipping_primaries",
			"log_shipping_primary_backupserver", "log_shipping_primary_databases",
			"log_shipping_primary_secondaries", "log_shipping_secondaries",
			"log_shipping_secondary", "log_shipping_secondary_databases",
			"logmarkhistory", "msdb_version", "MSdbms", "MSdbms_datatype",
			"MSdbms_datatype_mapping", "MSdbms_map",
			"restorefile", "restorefilegroup", "restorehistory", "smart_backup_files",
			"sqlagent_info", "suspect_pages", "sysalerts", "syscachedcredentials",
			"syscategories", "syscollector_blobs_internal", "syscollector_collection_items_internal",
			"syscollector_collection_sets_internal", "syscollector_collector_types_internal",
			"syscollector_config_store_internal", "syscollector_execution_log_internal",
			"syscollector_execution_stats_internal", "syscollector_tsql_query_collector",
			"sysdac_history_internal", "sysdac_instances_internal", "sysdbmaintplan_databases",
			"sysdbmaintplan_history", "sysdbmaintplan_jobs", "sysdbmaintplans",
			"sysdownloadlist", "sysjobactivity", "sysjobhistory", "sysjobs", "sysjobschedules",
			"sysjobservers", "sysjobsteps", "sysjobstepslogs", "sysmail_account", "sysmail_attachments",
			"sysmail_attachments_transfer", "sysmail_configuration", "sysmail_log", "sysmail_mailitems",
			"sysmail_principalprofile", "sysmail_profile", "sysmail_profileaccount", "sysmail_query_transfer",
			"sysmail_send_retries", "sysmail_server", "sysmail_servertype", "sysmaintplan_log",
			"sysmaintplan_logdetail", "sysmaintplan_subplans", "sysmanagement_shared_registered_servers_internal",
			"sysmanagement_shared_server_groups_internal", "sysnotifications", "sysoperators",
			"sysoriginatingservers", "syspolicy_conditions_internal", "syspolicy_configuration_internal",
			"syspolicy_execution_internal", "syspolicy_facet_events", "syspolicy_management_facets",
			"syspolicy_object_sets_internal", "syspolicy_policies_internal", "syspolicy_policy_categories_internal",
			"syspolicy_policy_category_subscriptions_internal", "syspolicy_policy_execution_history_details_internal",
			"syspolicy_policy_execution_history_internal", "syspolicy_system_health_state_internal",
			"syspolicy_target_set_levels_internal", "syspolicy_target_sets_internal", "sysproxies",
			"sysproxylogin", "sysproxysubsystem", "sysschedules", "syssessions", "sysssislog",
			"sysssispackagefolders", "sysssispackages", "syssubsystems", "systargetservergroupmembers",
			"systargetservergroups", "systargetservers", "systaskids", "sysutility_mi_configuration_internal",
			"sysutility_mi_cpu_stage_internal", "sysutility_mi_dac_execution_statistics_internal",
			"sysutility_mi_session_statistics_internal", "sysutility_mi_smo_objects_to_collect_internal",
			"sysutility_mi_smo_properties_to_collect_internal", "sysutility_mi_smo_stage_internal",
			"sysutility_mi_volumes_stage_internal", "sysutility_ucp_aggregated_dac_health_internal",
			"sysutility_ucp_aggregated_mi_health_internal", "sysutility_ucp_computer_cpu_health_internal",
			"sysutility_ucp_computers_stub", "sysutility_ucp_configuration_internal",
			"sysutility_ucp_cpu_utilization_stub", "sysutility_ucp_dac_file_space_health_internal",
			"sysutility_ucp_dac_health_internal", "sysutility_ucp_dacs_stub", "sysutility_ucp_databases_stub",
			"sysutility_ucp_datafiles_stub", "sysutility_ucp_filegroups_stub",
			"sysutility_ucp_filegroups_with_policy_violations_internal", "sysutility_ucp_health_policies_internal",
			"sysutility_ucp_logfiles_stub", "sysutility_ucp_managed_instances_internal",
			"sysutility_ucp_mi_database_health_internal", "sysutility_ucp_mi_file_space_health_internal",
			"sysutility_ucp_mi_health_internal", "sysutility_ucp_mi_volume_space_health_internal",
			"sysutility_ucp_policy_check_conditions_internal", "sysutility_ucp_policy_target_conditions_internal",
			"sysutility_ucp_policy_violations_internal", "sysutility_ucp_processing_state_internal",
			"sysutility_ucp_smo_servers_stub", "sysutility_ucp_snapshot_partitions_internal",
			"sysutility_ucp_space_utilization_stub", "sysutility_ucp_supported_object_types_internal",
			"sysutility_ucp_volumes_stub"
	);

	public SQLServerAdapter(@Inject RuntimeProperties runtimeProperties,
							@Inject(Constants.SERVER_DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
							@Inject(Constants.SERVER_USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
							@Inject(Constants.SERVER_TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
							@Inject(Constants.SERVER_RESOURCE_LOCATOR) ResourceLocator resourceLocator,
							@Inject ValueObjectTypeRegistry valueObjectTypeRegistry) {
		super(runtimeProperties, defaultExtendedTypes, userExtendedTypes, extendedTypeFactories, resourceLocator, valueObjectTypeRegistry);

		this.setSupportsBatchUpdates(true);
	}

    /**
     * Not supported, see: <a href="https://github.com/microsoft/mssql-jdbc/issues/245">mssql-jdbc #245</a>
     */
	@Override
	public boolean supportsGeneratedKeysForBatchInserts() {
		return false;
	}
	
	/**
	 * @since 4.2
	 */
	@Override
	public SQLTreeProcessor getSqlTreeProcessor() {
		if(getVersion() != null && getVersion() >= 12) {
			return new SQLServerTreeProcessorV12();
		}
		return new SQLServerTreeProcessor();
	}

	/**
	 * Uses SQLServerActionBuilder to create the right action.
	 *
	 * @since 1.2
	 */
	@Override
	public SQLAction getAction(Query query, DataNode node) {
		return query.createSQLAction(new SQLServerActionBuilder(node, getVersion()));
	}

	@Override
	public List<String> getSystemCatalogs() {
		return SYSTEM_CATALOGS;
	}

	@Override
	public List<String> getSystemSchemas() {
		return SYSTEM_SCHEMAS;
	}

	public Integer getVersion() {
		return version;
	}

	/**
	 * @since 4.2
	 * @param version of the server as provided by the JDBC driver
	 */
	public void setVersion(Integer version) {
		this.version = version;
	}
}
