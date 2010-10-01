package org.opensha.refFaultParamDb.tests.dao.db;

import static org.junit.Assert.*;

import java.sql.SQLException;

import javax.sql.rowset.CachedRowSet;

import org.junit.Before;
import org.junit.Test;
import org.opensha.refFaultParamDb.dao.db.DB_AccessAPI;
import org.opensha.refFaultParamDb.dao.db.DB_ConnectionPool;

import util.TestUtils;

public class TestDBConnectionOperational {

	private DB_AccessAPI db;
	
	@Before
	public void setUp() throws Exception {
	}
	
	@Test
	public void testDirectConnection3() throws Throwable {
		runTestTimed(DB_ConnectionPool.getDB3ReadOnlyConn());
	}
	
	@Test
	public void testServletConnection3() throws Throwable {
		runTestTimed(DB_ConnectionPool.getDB3ReadWriteConn());
	}
	
	@Test
	public void testDirectConnection2() throws Throwable {
		runTestTimed(DB_ConnectionPool.getDB2ReadOnlyConn());
	}
	
	@Test
	public void testServletConnection2() throws Throwable {
		runTestTimed(DB_ConnectionPool.getDB2ReadWriteConn());
	}
	
	private void runTestTimed(DB_AccessAPI db) throws Throwable {
		this.db = db;
		TestUtils.runTestWithTimer("runTest", this, 240);
	}
	
	@SuppressWarnings("unused")
	private void runTest() throws SQLException {
		String sql = "SELECT * FROM Fault_Model where rownum<=1";
		CachedRowSet rs = db.queryData(sql);
		rs.first();
	}

}
