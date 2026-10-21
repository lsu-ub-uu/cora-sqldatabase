package se.uu.ub.cora.sqldatabase;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.sqldatabase.connection.ParameterConnectionProviderImp;
import se.uu.ub.cora.sqldatabase.connection.SqlConnectionProvider;
import se.uu.ub.cora.sqldatabase.internal.DatabaseFacadeImp;
import se.uu.ub.cora.sqldatabase.sequence.Sequence;
import se.uu.ub.cora.sqldatabase.sequence.internal.SequenceImp;
import se.uu.ub.cora.sqldatabase.table.TableFacade;
import se.uu.ub.cora.sqldatabase.table.TableQuery;
import se.uu.ub.cora.sqldatabase.table.internal.TableFacadeImp;
import se.uu.ub.cora.sqldatabase.table.internal.TableQueryImp;
import se.uu.ub.cora.testspies.logger.LoggerFactorySpy;

public class RealDbTest {

	private LoggerFactorySpy loggerFactorySpy;
	private SqlDatabaseFactoryImp databaseFactory;

	@BeforeTest
	public void beforeTest() {
		databaseFactory = createDatabaseFactoryForSystemOne();
	}

	@BeforeMethod
	public void beforeMethod() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);

	}

	private SqlDatabaseFactoryImp createDatabaseFactoryForSystemOne() {
		SqlDatabaseFactoryImp factory = SqlDatabaseFactoryImp.usingUriAndUserAndPassword(
				"jdbc:postgresql://systemone-postgresql:5432/systemone", "systemone", "systemone");
		return factory;
	}

	@Test(enabled = false)
	private void testTransactionConflict() {
		DatabaseFacade databaseFacade = databaseFactory.factorDatabaseFacade();
		DatabaseFacade databaseFacade2 = databaseFactory.factorDatabaseFacade();

		String deleteSql = "delete from public.record where type='tramsType';";
		databaseFacade.executeSqlWithValues(deleteSql, Collections.emptyList());
		System.out.println("1");
		databaseFacade.startTransaction();
		System.out.println("2");
		databaseFacade2.startTransaction();
		System.out.println("3");

		insertIntoRecordUsingTypeAndIdAndDividerAndDataAsJson(databaseFacade, "tramsType",
				"tramsId", "test", "{\"trams1\":\"trams1\"}");
		System.out.println("4");

		insertIntoRecordUsingTypeAndIdAndDividerAndDataAsJson(databaseFacade2, "tramsType",
				"tramsId", "test", "{\"trams2\":\"trams2\"}");
		System.out.println("5");

		databaseFacade.endTransaction();
		System.out.println("6");

		databaseFacade2.endTransaction();
		System.out.println("7");
	}

	@Test
	public void testTransactionConflictWithTwoThreads() throws Exception {
		DatabaseFacade tx1 = databaseFactory.factorDatabaseFacade();
		DatabaseFacade tx2 = databaseFactory.factorDatabaseFacade();

		CountDownLatch tx2ReadyToInsert = new CountDownLatch(1);
		CountDownLatch allowTx2Insert = new CountDownLatch(1);

		AtomicReference<Throwable> tx2Failure = new AtomicReference<>();

		// Optional but recommended: fail fast instead of waiting forever
		// tx2.executeSqlWithValues("SET LOCAL lock_timeout = '3s'", Collections.emptyList());

		Thread t2 = new Thread(() -> {
			try {
				System.out.println("starting transaction thread 2");
				tx2.startTransaction();
				tx2ReadyToInsert.countDown(); // signal thread is ready
				allowTx2Insert.await(); // wait until tx1 insert is done
				System.out.println("delete thread 2");
				String deleteSql2 = "delete from public.record where type='tramsType'";
				// tx2.executeSqlWithValues(deleteSql2, Collections.emptyList());
				tx2.readOneRowOrFailUsingSqlAndValues("select * from public.record where type=?",
						List.of("tramsType"));

				System.out.println("insert thread 2");
				insertIntoRecordUsingTypeAndIdAndDividerAndDataAsJson(tx2, "tramsType", "tramsId",
						"test", "{\"trams2\":\"trams2\"}");
				System.out.println("delete and insert thread 2 DONE!");

				// If this succeeds unexpectedly, commit so cleanup is safe
				System.out.println("ending transaction thread 2");
				tx2.endTransaction();
			} catch (Exception e) {
				System.out.println("Retry" + e.getClass());
				System.out.println("Retry" + e.getCause().getMessage());
				SQLException cause = (SQLException) e.getCause();
				System.err.println("SQL state :" + cause.getSQLState());

				tx2Failure.set(e);
				try {
					tx2.rollback();
				} catch (Throwable ignored) {
				}
			}
		});

		t2.start();

		// Ensure t2 is waiting before we proceed
		assertTrue(tx2ReadyToInsert.await(2, TimeUnit.SECONDS), "t2 did not start in time");

		System.out.println("starting transaction thread 1");
		tx1.startTransaction();
		System.out.println("delete and insert thread 1");
		String deleteSql = "delete from public.record where type='tramsType'";
		tx1.executeSqlWithValues(deleteSql, Collections.emptyList());

		// tx1 inserts first and keeps transaction open
		insertIntoRecordUsingTypeAndIdAndDividerAndDataAsJson(tx1, "tramsType", "tramsId", "test",
				"{\"trams1\":\"trams1\"}");
		System.out.println("delete and insert thread 1 DONE!");

		// Let tx2 attempt conflicting insert (it will block until tx1 commits/rolls back)
		allowTx2Insert.countDown();

		// Give tx2 a short chance to hit the blocked state
		Thread.sleep(200);

		// Resolve blocker: commit tx1, then tx2 should fail with duplicate key
		System.out.println("ending transaction thread 1");
		tx1.endTransaction();

		t2.join(5000);
		assertFalse(t2.isAlive(), "t2 did not finish in time");

		Throwable failure = tx2Failure.get();
		assertNotNull(failure, "Expected tx2 to fail with unique constraint violation");

		// Adapt to your facade exception wrapping:
		// assertEquals(extractSqlState(failure), "23505");
	}

	@Test(enabled = false)
	private void testWithIn() {
		DatabaseFacade databaseFacade = databaseFactory.factorDatabaseFacade();
		String deleteSql = "delete from public.record;";
		databaseFacade.executeSqlWithValues(deleteSql, Collections.emptyList());

		insertIntoRecordUsingTypeAndIdAndDividerAndDataAsJson(databaseFacade, "recordType",
				"recordType", "cora", "{\"trams\":\"trams\"}");
		insertIntoRecordUsingTypeAndIdAndDividerAndDataAsJson(databaseFacade, "metadata",
				"recordType", "cora", "{\"trams\":\"trams\"}");
		insertIntoRecordUsingTypeAndIdAndDividerAndDataAsJson(databaseFacade, "otherType",
				"recordType", "cora", "{\"trams\":\"trams\"}");

		String sql = "select * from record where type in(?, ?);";
		List<Object> values = List.of("recordType", "metadata");
		List<Row> result = databaseFacade.readUsingSqlAndValues(sql, values);
		assertNotNull(result);
		assertEquals(result.size(), 2);

		TableFacadeImp tableFacade = TableFacadeImp.usingDatabaseFacade(databaseFacade);
		TableQuery recordTableQuery = databaseFactory.factorTableQuery("record");
		recordTableQuery.addCondition("type", values);

		long readNumberOfRows = tableFacade.readNumberOfRows(recordTableQuery);
		assertEquals(readNumberOfRows, 2);

		databaseFacade.executeSqlWithValues(deleteSql, Collections.emptyList());
	}

	@Test(enabled = false)
	private void testCreateSequence() {
		DatabaseFacadeImp databaseFacadeImp = (DatabaseFacadeImp) databaseFactory
				.factorDatabaseFacade();
		String name = "anotherGeneratorssss";

		// CREATE sequence
		String createSequence = "create sequence " + name + " start with 526;";
		databaseFacadeImp.executeSql(createSequence);

		// NEXTVALUE sequence
		String readSequence = "select nextval('public." + name + "');";
		Row row = databaseFacadeImp.readOneRowOrFailUsingSqlAndValues(readSequence,
				Collections.emptyList());
		Long valueByColumn = (Long) row.getValueByColumn("nextval");
		System.out.println(valueByColumn);
		System.err.println(valueByColumn.getClass());
		long a = valueByColumn.longValue();

		// DELETE sequence
		String deleteSequence = "drop sequence if exists " + name + ";";
		databaseFacadeImp.executeSql(deleteSequence);
	}

	@Test(enabled = false)
	private void testCreateSequenceImp() {
		DatabaseFacadeImp databaseFacadeImp = (DatabaseFacadeImp) databaseFactory
				.factorDatabaseFacade();
		String name = "lasquencia9";

		SequenceImp sequence = SequenceImp.usingDatabaseFacade(databaseFacadeImp);

		sequence.createSequence(name, -19);
		System.out.println(sequence.getCurrentValueForSequence(name));

		System.out.println(sequence.getNextValueForSequence(name));
		System.out.println(sequence.getCurrentValueForSequence(name));

		sequence.updateSequenceValue(name, 26);
		System.out.println(sequence.getNextValueForSequence(name));
		System.out.println(sequence.getCurrentValueForSequence(name));

		sequence.deleteSequence(name);

	}

	@Test(enabled = false)
	private void testReadNonExistingSequenceImp() {
		databaseFacadeImp = (DatabaseFacadeImp) databaseFactory.factorDatabaseFacade();
		String name = "lasquencia4499";

		SequenceImp sequence = SequenceImp.usingDatabaseFacade(databaseFacadeImp);
		//
		// sequence.createSequence(name, -19);
		// System.out.println(sequence.getCurrentValueForSequence(name));
		//
		// System.out.println(sequence.getNextValueForSequence(name));
		// System.out.println(sequence.getCurrentValueForSequence(name));
		//
		// sequence.updateSequenceValue(name, 26);
		// System.out.println(sequence.getNextValueForSequence(name));
		// System.out.println(sequence.getCurrentValueForSequence(name));
		//
		// sequence.deleteSequence(name);
		readCurrentNumberFromSequence(name);

	}

	DatabaseFacadeImp databaseFacadeImp;

	private long readCurrentNumberFromSequence(String id) {
		// String id = dataRecordGroup.getId();
		try (Sequence sequence = databaseFactory.factorSequence()) {
			return tryToReadCurrentValueAndIfFailsCreateSequenceAndRetry(id, sequence);
		} catch (Exception e) {
			// throw IdSourceException.withMessageAndException(
			// "Error reading current value for sequence with id: " + id, e);
			throw new RuntimeException("Error reading current value for sequence with id: " + id,
					e);
		}
	}

	private long tryToReadCurrentValueAndIfFailsCreateSequenceAndRetry(String id,
			Sequence sequence) {
		try {
			return sequence.getCurrentValueForSequence(id);
		} catch (Exception _) {
			createSequence(sequence, id);
			return sequence.getCurrentValueForSequence(id);
		}
	}

	private void createSequence(Sequence sequence, String id) {
		throw new RuntimeException("created crash");
		// sequence.createSequence(id, Long.valueOf(333));
	}

	private void insertIntoRecordUsingTypeAndIdAndDividerAndDataAsJson(
			DatabaseFacade databaseFacade, String recordType, String recordId, String dataDivider,
			String json) {
		String insertSql = "INSERT INTO public.record " + "(type, id, datadivider, data)"
				+ "VALUES(?, ?, ?, to_json(?::json));";
		List<Object> insertValues = List.of(recordType, recordId, dataDivider, json);
		databaseFacade.executeSqlWithValues(insertSql, insertValues);
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
