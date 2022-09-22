package se.uu.ub.cora.sqldatabase;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.sqldatabase.connection.ParameterConnectionProviderImp;
import se.uu.ub.cora.sqldatabase.connection.SqlConnectionProvider;
import se.uu.ub.cora.sqldatabase.internal.DatabaseFacadeImp;
import se.uu.ub.cora.sqldatabase.table.TableFacade;
import se.uu.ub.cora.sqldatabase.table.TableQuery;
import se.uu.ub.cora.sqldatabase.table.internal.TableQueryImp;
import se.uu.ub.cora.testspies.logger.LoggerFactorySpy;

public class RealDbTest {

	private LoggerFactorySpy loggerFactorySpy;

	@BeforeMethod
	public void beforeMethod() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);

	}

	@Test(enabled = true)
	private void testWithIn() {
		SqlConnectionProvider sProvider = ParameterConnectionProviderImp.usingUriAndUserAndPassword(
				"jdbc:postgresql://systemone-cora-docker-postgresql:5432/systemone", "systemone",
				"systemone");
		DatabaseFacadeImp dataReaderImp = DatabaseFacadeImp.usingSqlConnectionProvider(sProvider);
		String sql = "select * from record where type in(?);";
		List<Object> values = new ArrayList<>();
		List<Row> result = dataReaderImp.readUsingSqlAndValues(sql, values);
		assertNotNull(result);
	}

	@Test(enabled = false)
	private void test() {
		SqlConnectionProvider sProvider = ParameterConnectionProviderImp.usingUriAndUserAndPassword(
				"jdbc:postgresql://alvin-cora-docker-postgresql:5432/alvin", "alvin", "alvin");
		DatabaseFacadeImp dataReaderImp = DatabaseFacadeImp.usingSqlConnectionProvider(sProvider);
		String sql = "select * from country;";
		List<Object> values = new ArrayList<>();
		List<Row> result = dataReaderImp.readUsingSqlAndValues(sql, values);
		assertNotNull(result);
	}

	@Test(enabled = false)
	private void testWithWhere() {
		SqlConnectionProvider sProvider = ParameterConnectionProviderImp.usingUriAndUserAndPassword(
				"jdbc:postgresql://diva-cora-docker-postgresql:5432/diva", "diva", "diva");
		DatabaseFacadeImp dataReaderImp = DatabaseFacadeImp.usingSqlConnectionProvider(sProvider);
		String sql = "select * from organisation where organisation_id = ?;";
		// String sql = "select * from organisation ;";
		List<Object> values = new ArrayList<>();
		values.add(51);
		List<Row> result = dataReaderImp.readUsingSqlAndValues(sql, values);
		assertNotNull(result);
	}

	@Test(enabled = false)
	private void testWithWhereName() {
		SqlConnectionProvider sProvider = ParameterConnectionProviderImp.usingUriAndUserAndPassword(
				"jdbc:postgresql://diva-cora-docker-postgresql:5432/diva", "diva", "diva");
		DatabaseFacadeImp dataReaderImp = DatabaseFacadeImp.usingSqlConnectionProvider(sProvider);
		String sql = "select * from organisation where organisation_name= ?;";
		// String sql = "select * from organisation ;";
		List<Object> values = new ArrayList<>();
		values.add("Stockholms organisation");
		List<Row> result = dataReaderImp.readUsingSqlAndValues(sql, values);
		assertNotNull(result);
	}

	@Test(enabled = false)
	private void testReadJson() {
		SqlConnectionProvider sProvider = ParameterConnectionProviderImp.usingUriAndUserAndPassword(
				"jdbc:postgresql://diva-cora-docker-postgresql:5432/diva", "diva", "diva");
		DatabaseFacadeImp dataReaderImp = DatabaseFacadeImp.usingSqlConnectionProvider(sProvider);
		String sql = "select * from record_person where id= ?";
		// String sql = "select * from organisation ;";
		List<Object> values = new ArrayList<>();
		values.add("authority-person:111");
		Row result = dataReaderImp.readOneRowOrFailUsingSqlAndValues(sql, values);
		assertNotNull(result);
		assertEquals((String) result.getValueByColumn("record"), "");
	}

	@Test(enabled = false)
	private void testWithWherenot_eligible() {
		SqlConnectionProvider sProvider = ParameterConnectionProviderImp.usingUriAndUserAndPassword(
				// "jdbc:postgresql://diva-cora-docker-postgresql:5432/diva", "diva", "diva");
				"jdbc:postgresql://diva-docker-mock-classic-postgresql:5432/diva", "diva", "diva");
		DatabaseFacadeImp dataReaderImp = DatabaseFacadeImp.usingSqlConnectionProvider(sProvider);
		// String sql = "select * from organisation where not_eligible= ?;";
		String sql = "select * from ? where not_eligible= ?;";
		List<Object> values = new ArrayList<>();
		values.add("organisation");
		values.add(false);
		List<Row> result = dataReaderImp.readUsingSqlAndValues(sql, values);
		assertNotNull(result);
	}

	@Test(enabled = false)
	public void testTableFacadeInsertCrap() throws Exception {
		SqlDatabaseFactory factory = SqlDatabaseFactoryImp.usingUriAndUserAndPassword(
				"jdbc:postgresql://diva-docker-mock-classic-postgresql:5432/diva", "diva", "diva");
		TableFacade tableFacade = factory.factorTableFacade();
		TableQuery query = TableQueryImp.usingTableName("organisation");
		query.addParameter("organisation_id", 666);
		query.addParameter("organisation_name_locale", "sv");
		query.addParameter("organisation_type_id", 54);
		query.addParameter("domain", "test");
		query.addParameter("organisation_name", "mail@yahoo.se; DROP person2");
		tableFacade.insertRowUsingQuery(query);
	}

	@Test(enabled = false)
	public void testTableFacadeUpdateCrap() throws Exception {
		SqlDatabaseFactory factory = SqlDatabaseFactoryImp.usingUriAndUserAndPassword(
				"jdbc:postgresql://diva-docker-mock-classic-postgresql:5432/diva", "diva", "diva");
		TableFacade tableFacade = factory.factorTableFacade();
		TableQuery query = TableQueryImp.usingTableName("organisation");
		query.addParameter("organisation_aaid", 666);
		query.addParameter("organisation_name_locale", "sv");
		query.addParameter("organisation_type_id", 54);
		query.addParameter("domain", "test");
		query.addParameter("organisation_name", "mail@yahoo.se; DROP person2");
		query.addCondition("organisation_aaid", 667);
		tableFacade.updateRowsUsingQuery(query);
	}

	@Test(enabled = false)
	public void testAutoClosableNoCalls() throws Exception {
		SqlDatabaseFactory factory = SqlDatabaseFactoryImp.usingUriAndUserAndPassword(
				"jdbc:postgresql://diva-docker-mock-classic-postgresql:5432/diva", "diva", "diva");
		try (TableFacade tableFacade = factory.factorTableFacade()) {

		}
		assertTrue(true);
	}

	@Test(enabled = false)
	public void testTableFacadeInsertCrap2() throws Exception {
		SqlDatabaseFactory factory = SqlDatabaseFactoryImp.usingUriAndUserAndPassword(
				"jdbc:postgresql://diva-docker-mock-classic-postgresql:5432/diva", "diva", "diva");
		TableFacade tableFacade = factory.factorTableFacade();
		TableQuery query = TableQueryImp.usingTableName("organisation");
		query.addCondition("organisation_name", "somerecord; DROP person2");
		tableFacade.readRowsForQuery(query);
	}

	@Test(enabled = false)
	private void testUpdate() {
		SqlConnectionProvider sProvider = ParameterConnectionProviderImp.usingUriAndUserAndPassword(
				"jdbc:postgresql://alvin-cora-docker-postgresql:5432/alvin", "alvin", "alvin");
		DatabaseFacadeImp dataReaderImp = DatabaseFacadeImp.usingSqlConnectionProvider(sProvider);
		String sql = "update country set defaultname = ? where alpha2code = 'SE';";
		List<Object> values = new ArrayList<>();
		values.add("fake name se");
		List<Row> result = dataReaderImp.readUsingSqlAndValues(sql, values);
		assertNotNull(result);
	}

	@Test(enabled = false)
	private void testWithWhereAlvin() {
		SqlConnectionProvider sProvider = ParameterConnectionProviderImp.usingUriAndUserAndPassword(
				"jdbc:postgresql://alvin-cora-docker-postgresql:5432/alvin", "alvin", "alvin");
		DatabaseFacadeImp dataReaderImp = DatabaseFacadeImp.usingSqlConnectionProvider(sProvider);
		String sql = "select * from country where alpha2code= 'SE';";
		// String sql = "select * from organisation ;";
		List<Object> values = new ArrayList<>();
		// values.add(51);
		List<Row> result = dataReaderImp.readUsingSqlAndValues(sql, values);
		assertNotNull(result);
	}

	@Test(enabled = false)
	private void testReadAlvinUser() {
		SqlConnectionProvider sProvider = ParameterConnectionProviderImp.usingUriAndUserAndPassword(
				"jdbc:postgresql://dev-alvin-postgresql:5432/alvin", "alvinAdmin", "alvinAdmin");
		DatabaseFacadeImp dataReaderImp = DatabaseFacadeImp.usingSqlConnectionProvider(sProvider);
		String sql = "select u.*, ar.group_id from alvin_seam_user u "
				+ "left join alvin_role ar on u.id = ar.user_id "
				// + "where ar.group_id = 54 and u.userid='olfel499' and u.domain='uu';";
				+ "where u.userid='olfel499' and u.domain='uu';";
		// String sql = "select * from organisation ;";
		List<Object> values = new ArrayList<>();
		// values.add(51);
		List<Row> result = dataReaderImp.readUsingSqlAndValues(sql, values);
		for (Row row : result) {
			System.out.println(row);
		}
		assertNotNull(result);
	}

}
