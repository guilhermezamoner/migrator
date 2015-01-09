package br.eti.clairton.migrator;

import static javax.enterprise.inject.spi.CDI.current;
import static liquibase.database.DatabaseFactory.getInstance;

import java.sql.Connection;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.transaction.Transactional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;

@ApplicationScoped
public class Migrator implements javax.enterprise.inject.spi.Extension {
	private final Logger logger = LogManager.getLogger(getClass().getName());

	private final String changelog = "db/changelogs/changelog-main.xml";

	private Liquibase liquibase;

	public void init(final @Observes AfterDeploymentValidation adv)
			throws Exception {
		logger.info("Iniciando migração do banco de dados");
		migrate();
		current().select(Inserter.class).get().init();
	}

	@Transactional
	public void migrate() {
		try {
			final Connection connection = current().select(Connection.class)
					.get();
			Config config = current().select(Config.class).get();
			migrate(connection, config);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public void migrate(Connection connection, Config config) {
		try {
			final DatabaseConnection jdbcConnection = new JdbcConnection(
					connection);
			final Database database = getInstance()
					.findCorrectDatabaseImplementation(jdbcConnection);
			final ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor(
					getClass().getClassLoader());
			liquibase = new Liquibase(changelog, resourceAccessor, database);
			if (config.isDropAll()) {
				logger.info("Deletando objetos");
				liquibase.dropAll();
			}
			final String context = "";
			logger.info("Rodando changesets");
			liquibase.update(context);
			logger.info("Aplicado changesets");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
