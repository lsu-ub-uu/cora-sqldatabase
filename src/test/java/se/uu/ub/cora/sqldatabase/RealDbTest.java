package se.uu.ub.cora.sqldatabase;

import static org.testng.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.sqldatabase.connection.ParameterConnectionProviderImp;
import se.uu.ub.cora.sqldatabase.connection.SqlConnectionProvider;
import se.uu.ub.cora.sqldatabase.data.DatabaseFacadeImp;
import se.uu.ub.cora.sqldatabase.log.LoggerFactorySpy;

public class RealDbTest {

	private LoggerFactorySpy loggerFactorySpy;

	@BeforeMethod
	public void beforeMethod() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);

	}

	@Test(enabled = false)
	private void test() {
		SqlConnectionProvider sProvider = ParameterConnectionProviderImp.usingUriAndUserAndPassword(
				"jdbc:postgresql://alvin-cora-docker-postgresql:5432/alvin", "alvin", "alvin");
		DatabaseFacadeImp dataReaderImp = DatabaseFacadeImp.usingSqlConnectionProvider(sProvider);
		String sql = "select * from country;";
		List<Object> values = new ArrayList<>();
		List<Row> result = dataReaderImp.readUsingSqlAndValues(sql,
				values);
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
		List<Row> result = dataReaderImp.readUsingSqlAndValues(sql,
				values);
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
		List<Row> result = dataReaderImp.readUsingSqlAndValues(sql,
				values);
		assertNotNull(result);
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
		List<Row> result = dataReaderImp.readUsingSqlAndValues(sql,
				values);
		assertNotNull(result);
	}

	@Test(enabled = false)
	private void testUpdate() {
		SqlConnectionProvider sProvider = ParameterConnectionProviderImp.usingUriAndUserAndPassword(
				"jdbc:postgresql://alvin-cora-docker-postgresql:5432/alvin", "alvin", "alvin");
		DatabaseFacadeImp dataReaderImp = DatabaseFacadeImp.usingSqlConnectionProvider(sProvider);
		String sql = "update country set defaultname = ? where alpha2code = 'SE';";
		List<Object> values = new ArrayList<>();
		values.add("fake name se");
		List<Row> result = dataReaderImp.readUsingSqlAndValues(sql,
				values);
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
		List<Row> result = dataReaderImp.readUsingSqlAndValues(sql,
				values);
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
		List<Row> result = dataReaderImp.readUsingSqlAndValues(sql,
				values);
		for (Row row : result) {
			System.out.println(row);
		}
		assertNotNull(result);
	}

}
