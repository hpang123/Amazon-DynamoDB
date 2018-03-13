package com.amazonaws.samples;

import java.util.ArrayList;
import java.util.Arrays;
/*
 * Copyright 2012-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.util.TableUtils;

/**
 * This sample demonstrates how to perform a few simple operations with the
 * Amazon DynamoDB service.
 */
public class AmazonDynamoDBSample {

	/*
	 * Before running the code: Fill in your AWS access credentials in the provided
	 * credentials file template, and be sure to move the file to the default
	 * location (C:\\Users\\Ray\\.aws\\credentials) where the sample code will load
	 * the credentials from.
	 * https://console.aws.amazon.com/iam/home?#security_credential
	 *
	 * WARNING: To avoid accidental leakage of your credentials, DO NOT keep the
	 * credentials file in your source directory.
	 */

	static AmazonDynamoDB dynamoDB;

	/**
	 * The only information needed to create a client are security credentials
	 * consisting of the AWS Access Key ID and Secret Access Key. All other
	 * configuration, such as the service endpoints, are performed automatically.
	 * Client parameters, such as proxies, can be specified in an optional
	 * ClientConfiguration object when constructing a client.
	 *
	 * @see com.amazonaws.auth.BasicAWSCredentials
	 * @see com.amazonaws.auth.ProfilesConfigFile
	 * @see com.amazonaws.ClientConfiguration
	 */
	private static void init() throws Exception {
		/*
		 * The ProfileCredentialsProvider will return your [default] credential profile
		 * by reading from the credentials file located at
		 * (C:\\Users\\Ray\\.aws\\credentials).
		 */
		ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();
		try {
			credentialsProvider.getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
					+ "Please make sure that your credentials file is at the correct "
					+ "location (C:\\Users\\Ray\\.aws\\credentials), and is in valid format.", e);
		}
		dynamoDB = AmazonDynamoDBClientBuilder.standard().withCredentials(credentialsProvider).withRegion("us-east-2")
				.build();
	}

	/*
	 * To run local DynamoDB
	 * Go to c:\dynamodb
	 * java -Djava.library.path=./DynamoDBLocal_lib -jar DynamoDBLocal.jar -port 8888
	 */
	private static void initLocal() throws Exception {
		/*
		 * The ProfileCredentialsProvider will return your [default] credential profile
		 * by reading from the credentials file located at
		 * (C:\\Users\\Ray\\.aws\\credentials).
		 */
		ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();
		try {
			credentialsProvider.getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
					+ "Please make sure that your credentials file is at the correct "
					+ "location (C:\\Users\\Ray\\.aws\\credentials), and is in valid format.", e);
		}
		
		EndpointConfiguration endpointConfiguration = new EndpointConfiguration("http://localhost:8888", null);
		
		dynamoDB = AmazonDynamoDBClientBuilder.standard().withCredentials(credentialsProvider).withEndpointConfiguration(endpointConfiguration)
				.build();
	}
	
	public static void main(String[] args) throws Exception {
		
		initLocal();
		//init();
		try {
			String tableName = "my-favorite-movies-table";

			// Create a table with a primary hash key named 'name', which holds a string
			CreateTableRequest createTableRequest = new CreateTableRequest().withTableName(tableName)
					/* for Primary key: partiion key, type is Hash not Range of sort key */
					.withKeySchema(new KeySchemaElement().withAttributeName("name").withKeyType(KeyType.HASH)) 
					.withAttributeDefinitions(new AttributeDefinition().withAttributeName("name")
							/* S - String; B - Binary; N - Number  */
							.withAttributeType(ScalarAttributeType.S))
					.withProvisionedThroughput(
							new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L));

			// Create table if it does not exist yet
			TableUtils.createTableIfNotExists(dynamoDB, createTableRequest);
			// wait for the table to move into ACTIVE state
			System.out.println("Waiting for " + tableName + " to become ACTIVE...");
			TableUtils.waitUntilActive(dynamoDB, tableName);

			// Describe our new table
			DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
			TableDescription tableDescription = dynamoDB.describeTable(describeTableRequest).getTable();
			System.out.println("Table Description: " + tableDescription);

			// Add an item
			Map<String, AttributeValue> item = newItem("Bill & Ted's Excellent Adventure", 1989, "****", "James",
					"Sara");
			PutItemRequest putItemRequest = new PutItemRequest(tableName, item);
			PutItemResult putItemResult = dynamoDB.putItem(putItemRequest);
			System.out.println("Result: " + putItemResult);

			// Add another item. If the key is already exist, it does update
			item = newItem("Airplane", 1981, "*****", "James", "Billy Bob");
			putItemRequest = new PutItemRequest(tableName, item);
			putItemResult = dynamoDB.putItem(putItemRequest);
			System.out.println("Result: " + putItemResult);

			// Scan items for movies with a year attribute greater than 1985
			HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
			Condition condition = new Condition().withComparisonOperator(ComparisonOperator.GT.toString())
					.withAttributeValueList(new AttributeValue().withN("1985"));
			scanFilter.put("year", condition);
			ScanRequest scanRequest = new ScanRequest(tableName).withScanFilter(scanFilter);
			ScanResult scanResult = dynamoDB.scan(scanRequest);
			System.out.println("Result: " + scanResult);

		} catch (AmazonServiceException ase) {
			System.out.println("Caught an AmazonServiceException, which means your request made it "
					+ "to AWS, but was rejected with an error response for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			System.out.println("Caught an AmazonClientException, which means the client encountered "
					+ "a serious internal problem while trying to communicate with AWS, "
					+ "such as not being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
		}
	}

	private static Map<String, AttributeValue> newItem(String name, int year, String rating, String... fans) {
		Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
		item.put("name", new AttributeValue(name)); //default string
		item.put("year", new AttributeValue().withN(Integer.toString(year))); //Number
		item.put("rating", new AttributeValue(rating));
		item.put("fans", new AttributeValue().withSS(fans)); //String set

		return item;
	}
	
	/* Other examples that are from the book */
	private static ProvisionedThroughput getProvisionedThroughput() {
		ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput()
				.withReadCapacityUnits(2L).withWriteCapacityUnits(2L);
		return provisionedThroughput;
	}

	private static ArrayList<KeySchemaElement> getTableKeySchema() {
		ArrayList<KeySchemaElement> ks = new ArrayList<KeySchemaElement>();
		ks.add(new KeySchemaElement().withAttributeName("hashKey").withKeyType(
				KeyType.HASH));
		ks.add(new KeySchemaElement().withAttributeName("rangeKey") //For sort key
				.withKeyType(KeyType.RANGE));
		return ks;
	}

	private static ArrayList<AttributeDefinition> getTableAttributes() {
		ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
		attributeDefinitions.add(new AttributeDefinition().withAttributeName(
				"hashKey").withAttributeType("S"));
		attributeDefinitions.add(new AttributeDefinition().withAttributeName(
				"rangeKey").withAttributeType("N"));
		return attributeDefinitions;

	}

	private static void putItems(String tableName) {
		Map<String, AttributeValue> item1 = new HashMap<String, AttributeValue>();
		item1.put("hashKey", new AttributeValue().withS("hash1"));
		item1.put("rangeKey", new AttributeValue().withN("1"));
		item1.put("stringSet", new AttributeValue().withSS(Arrays.asList(
				"string1", "string2")));
		item1.put("numberSet",
				new AttributeValue().withNS(Arrays.asList("3", "2", "1"))); //Number set
		PutItemRequest putItemRequest = new PutItemRequest().withTableName(
				tableName).withItem(item1);
		dynamoDB.putItem(putItemRequest);
	}


}
