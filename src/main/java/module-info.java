module se.uu.ub.cora.sqldatabase {
	requires transitive java.naming;
	requires transitive java.sql;
	requires se.uu.ub.cora.logger;

	exports se.uu.ub.cora.connection;
	exports se.uu.ub.cora.sqldatabase;
}