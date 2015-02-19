/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fr.tours.polytech.DI.RFID.utils;

import com.mysql.jdbc.exceptions.jdbc4.MySQLNonTransientConnectionException;
import fr.tours.polytech.DI.RFID.objects.Student;

import java.sql.*;
import java.util.logging.Level;

/**
 * Class that allow us to interact with a SQL database.
 *
 * @author COLEAU Victor, COUCHOUD Thomas
 */
public class SQLManager
{
	private String UID_LABEL = "UID", NAME_LABEL = "Name", STAFF_LABEL = "Staff";
	private Connection connection;
	private String tableName;
	private String databaseURL;
	private int port;
	private String databaseName;
	private String user;
	private String password;

	/**
	 * Constructor.
	 *
	 * @param databaseURL The URL of the database.
	 * @param port The port of the database.
	 * @param databaseName The database name.
	 * @param user The username.
	 * @param password The password for this user.
	 */
	public SQLManager(String databaseURL, int port, String databaseName, String user, String password)
	{
		this.databaseURL = databaseURL;
		this.port = port;
		this.databaseName = databaseName;
		this.user = user;
		this.password = password;
		login();
		Utils.logger.log(Level.INFO, "Initializing SQL connection...");
		this.tableName = "Users";
		createBaseTable();
	}

	public void addStudentToDatabase(Student student)
	{
		sendUpdateRequest("INSERT INTO " + this.tableName + " (" + this.UID_LABEL + "," + this.NAME_LABEL + "," + this.STAFF_LABEL + ") VALUES(\"" + student.getRawUid() + "\",\"" + student.getName() + "\",\"" + student.isStaffSQL() + "\")");
	}

	/**
	 * Used to retrieve a student from the database by his name.
	 *
	 * @param name The name of the student.
	 * @return The student corresponding, null if not found.
	 */
	public Student getStudentByName(String name)
	{
		ResultSet result = sendQueryRequest("SELECT " + this.UID_LABEL + ", " + this.STAFF_LABEL + " FROM " + this.tableName + " WHERE " + this.NAME_LABEL + " = \"" + name + "\";");
		try
		{
			if(result.next())
				return new Student(result.getString(this.UID_LABEL), name, result.getInt(this.STAFF_LABEL) == 1);
		}
		catch(SQLException exception)
		{
			Utils.logger.log(Level.WARNING, "", exception);
		}
		catch(NullPointerException exception)
		{}
		return null;
	}

	/**
	 * Used to retrieve a student from the database by his UID.
	 *
	 * @param uid The UID of the student.
	 * @return The student corresponding, null if not found.
	 */
	public Student getStudentByUID(String uid)
	{
		ResultSet result = sendQueryRequest("SELECT " + this.NAME_LABEL + ", " + this.STAFF_LABEL + " FROM " + this.tableName + " WHERE " + this.UID_LABEL + " = \"" + uid + "\";");
		try
		{
			if(result.next())
				return new Student(uid, result.getString(this.NAME_LABEL), result.getInt(this.STAFF_LABEL) == 1);
		}
		catch(SQLException exception)
		{
			Utils.logger.log(Level.WARNING, "", exception);
		}
		return null;
	}

	/**
	 * Used to send a query request to the database.
	 *
	 * @param request The request to send.
	 * @return The result of the query.
	 *
	 * @see ResultSet
	 */
	public synchronized ResultSet sendQueryRequest(String request)
	{
		return sendQueryRequest(request, true);
	}

	/**
	 * Used to send an update request to the database.
	 *
	 * @param request The request to send.
	 * @return How many lines were modified by the request.
	 */
	public synchronized int sendUpdateRequest(String request)
	{
		return sendUpdateRequest(request, true);
	}

	/**
	 * Used to create the default database.
	 *
	 * @return How many lines were modified by the request.
	 */
	private int createBaseTable()
	{
		return sendUpdateRequest("CREATE TABLE IF NOT EXISTS " + this.tableName + "(" + this.UID_LABEL + " varchar(18), " + this.NAME_LABEL + " varchar(255), " + this.STAFF_LABEL + " tinyint(1), PRIMARY KEY (" + this.UID_LABEL + ")) ENGINE=InnoDB DEFAULT CHARSET=utf8;");
	}

	/**
	 * Used to establish a connection with the database.
	 */
	private void login()
	{
		try
		{
			this.connection = DriverManager.getConnection("jdbc:mysql://" + this.databaseURL + ":" + this.port + "/" + this.databaseName, this.user, this.password);
		}
		catch(SQLException e)
		{
			Utils.logger.log(Level.WARNING, "Error connecting to SQL database!", e);
		}
	}

	/**
	 * Used to send a query request to the database.
	 *
	 * @param request The request to send.
	 * @param retry Should retry to send the request another time if it failed?
	 * @return The result of the query.
	 *
	 * @see ResultSet
	 */
	private ResultSet sendQueryRequest(String request, boolean retry)
	{
		if(this.connection == null)
			return null;
		Utils.logger.log(Level.INFO, "Sending MYSQL request...: " + request);
		ResultSet result = null;
		try
		{
			Statement statement = this.connection.createStatement();
			result = statement.executeQuery(request);
		}
		catch(MySQLNonTransientConnectionException e)
		{
			login();
			if(retry)
				return sendQueryRequest(request, false);
		}
		catch(SQLException exception)
		{
			Utils.logger.log(Level.WARNING, "SQL ERROR", exception);
		}
		return result;
	}

	/**
	 * Used to send an update request to the database.
	 *
	 * @param request The request to send.
	 * @param retry Should retry to send the request another time if it failed?
	 * @return How many lines were modified by the request.
	 */
	private int sendUpdateRequest(String request, boolean retry)
	{
		if(this.connection == null)
			return 0;
		Utils.logger.log(Level.INFO, "Sending MYSQL update...: " + request);
		int result = 0;
		try
		{
			Statement statement = this.connection.createStatement();
			result = statement.executeUpdate(request);
		}
		catch(MySQLNonTransientConnectionException e)
		{
			login();
			if(retry)
				return sendUpdateRequest(request, false);
		}
		catch(SQLException exception)
		{
			Utils.logger.log(Level.WARNING, "SQL ERROR", exception);
		}
		return result;
	}
}
