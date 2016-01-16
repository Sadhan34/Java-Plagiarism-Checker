package copyleaks.sdk.api;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import copyleaks.sdk.api.helpers.HttpURLConnection.CopyleaksClient;
import copyleaks.sdk.api.helpers.HttpURLConnection.HttpURLConnectionHelper;
import copyleaks.sdk.api.RequestMethod;
import copyleaks.sdk.api.exceptions.CommandFailedException;
import copyleaks.sdk.api.exceptions.SecurityTokenException;
import copyleaks.sdk.api.models.LoginToken;
import copyleaks.sdk.api.models.ResultRecord;
import copyleaks.sdk.api.models.responses.CheckStatusResponse;
import copyleaks.sdk.api.models.responses.CreateResourceResponse;
import copyleaks.sdk.api.models.responses.ProcessInList;

public class CopyleaksProcess implements Comparable<CopyleaksProcess>, Serializable
{
	/**
	 * For 'Serializable' implementation.
	 */
	private static final long serialVersionUID = 1L;

	public UUID PID;

	/**
	 * Get process ID
	 * 
	 * @return The process ID
	 */
	public UUID getPID()
	{
		return PID;
	}

	private void setPID(UUID processId)
	{
		PID = processId;
	}

	private Date CreationTimeUTC;

	/**
	 * Get the process creation time
	 * 
	 * @return Process creation time
	 */
	public Date getCreationTimeUTC()
	{
		return CreationTimeUTC;
	}

	private void setCreationTimeUTC(Date creationTimeUTC)
	{
		CreationTimeUTC = creationTimeUTC;
	}

	private boolean ListProcesses_IsCompleted = false;
	
	private HashMap<String, String> CustomFields;

	public HashMap<String, String> getCustomFields()
	{
		return CustomFields;
	}

	private void setCustomFields(HashMap<String, String> value)
	{
		this.CustomFields = value;
	}

	private LoginToken SecurityToken;

	private LoginToken getSecurityToken()
	{
		return SecurityToken;
	}

	private void setSecurityToken(LoginToken securityToken)
	{
		SecurityToken = securityToken;
	}

	CopyleaksProcess(LoginToken authorizationToken, ProcessInList process)
	{
		this.setPID(process.getProcessId());
		this.setCreationTimeUTC(process.getCreationTimeUTC());
		this.setSecurityToken(authorizationToken);
		this.setCustomFields(process.getCustomFields());
		this.ListProcesses_IsCompleted = process.getStatus().equalsIgnoreCase("finished");
	}

	CopyleaksProcess(LoginToken authorizationToken, CreateResourceResponse response,
			HashMap<String, String> customFields)
	{
		this.setPID(response.getProcessId());
		this.setCreationTimeUTC(response.getCreationTimeUTC());
		this.setSecurityToken(authorizationToken);
		this.setCustomFields(customFields);
	}

	/**
	 * Get process progress information
	 * 
	 * @return process progress (out of 100).
	 * @throws CommandFailedException
	 *             This exception is thrown if an exception situation occured
	 *             during the processing of a command
	 * @throws SecurityTokenException
	 *             The login-token is undefined or expired
	 */
	public int getCurrentProgress() throws SecurityTokenException, CommandFailedException
	{
		if (this.ListProcesses_IsCompleted)
		{
			return 100;
		}
		
		LoginToken.ValidateToken(this.getSecurityToken());

		URL url;
		HttpURLConnection conn = null;
		Gson gson = new GsonBuilder().create();
		String json;
		try
		{
			url = new URL(String.format("%1$s/%2$s/%3$s/%4$s/status", Settings.ServiceEntryPoint,
					Settings.ServiceVersion, Settings.ServicePage, getPID()));
			conn = CopyleaksClient.getClient(url, this.getSecurityToken(), RequestMethod.GET, HttpContentTypes.Json,
					HttpContentTypes.TextPlain);
			
			if (conn.getResponseCode() != 200)
				throw new CommandFailedException(conn);

			try (InputStream inputStream = new BufferedInputStream(conn.getInputStream()))
			{
				json = HttpURLConnectionHelper.convertStreamToString(inputStream);
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e.getMessage());
		}
		finally
		{
			if (conn != null)
				conn.disconnect();
		}
		CheckStatusResponse response = gson.fromJson(json, CheckStatusResponse.class);
		return response.getProgressPercents();
	}

	/**
	 * Get the scan results from the server
	 * 
	 * @return Scan results
	 * @throws CommandFailedException
	 *             This exception is thrown if an exception situation occured
	 *             during the processing of a command
	 * @throws SecurityTokenException
	 *             The login-token is undefined or expired
	 */
	public ResultRecord[] GetResults() throws SecurityTokenException, CommandFailedException
	{
		LoginToken.ValidateToken(this.getSecurityToken());

		String json;
		URL url;
		HttpURLConnection conn = null;
		Gson gson = new GsonBuilder().create();
		try
		{
			url = new URL(String.format("%1$s/%2$s/%3$s/%4$s/result", Settings.ServiceEntryPoint,
					Settings.ServiceVersion, Settings.ServicePage, getPID()));
			conn = CopyleaksClient.getClient(url, this.getSecurityToken(), RequestMethod.GET, HttpContentTypes.Json,
					HttpContentTypes.Json);
			
			if (conn.getResponseCode() != 200)
				throw new CommandFailedException(conn);

			try (InputStream inputStream = new BufferedInputStream(conn.getInputStream()))
			{
				json = HttpURLConnectionHelper.convertStreamToString(inputStream);
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e.getMessage());
		}
		finally
		{
			if (conn != null)
				conn.disconnect();
		}

		ResultRecord[] results = gson.fromJson(json, ResultRecord[].class);
		Arrays.sort(results, Collections.reverseOrder());
		return results;
	}

	/**
	 * Deletes the process once it has finished running
	 * 
	 * @throws CommandFailedException
	 *             This exception is thrown if an exception situation occured
	 *             during the processing of a command
	 * @throws SecurityTokenException
	 *             The login-token is undefined or expired
	 */
	public void Delete() throws SecurityTokenException, CommandFailedException
	{
		LoginToken.ValidateToken(this.getSecurityToken());

		URL url;
		HttpURLConnection conn = null;
		try
		{
			url = new URL(String.format("%1$s/%2$s/%3$s/%4$s/delete", Settings.ServiceEntryPoint,
					Settings.ServiceVersion, Settings.ServicePage, this.PID));
			conn = CopyleaksClient.getClient(url, this.getSecurityToken(), RequestMethod.DELETE, HttpContentTypes.Json,
					HttpContentTypes.Json);
			if (conn.getResponseCode() != 200)
				throw new CommandFailedException(conn);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e.getMessage());
		}
		finally
		{
			if (conn != null)
				conn.disconnect();
		}
	}

	@Override
	public String toString()
	{
		return this.getPID().toString();
	}

	@Override
	public int compareTo(CopyleaksProcess process)
	{
		return this.getCreationTimeUTC().compareTo(process.CreationTimeUTC);
	}
}