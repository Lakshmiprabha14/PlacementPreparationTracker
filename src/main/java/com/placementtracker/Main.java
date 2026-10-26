
package com.placementtracker;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        String uri = System.getenv("MONGODB_URI");

        if (uri == null || uri.isBlank()) {
            System.out.println("Please configure MONGODB_URI.");
            return;
        }

        try (MongoClient client = MongoClients.create(uri);
             Scanner scanner = new Scanner(System.in)) {

            MongoDatabase database =
                    client.getDatabase("PlacementTrackerDB");

            MongoCollection<Document> collection =
                    database.getCollection("topics");

            while (true) {
                System.out.println("\n=== Placement Preparation Tracker ===");
                System.out.println("1. Add Topic");
                System.out.println("2. View All Topics");
                System.out.println("3. Update Topic Status");
                System.out.println("4. Delete Topic");
                System.out.println("5. Exit");
                System.out.print("Choose an option: ");

                String choice = scanner.nextLine().trim();

                switch (choice) {

                    case "1":
                        System.out.print("Enter topic name: ");
                        String name = scanner.nextLine().trim();

                        if (name.isEmpty()) {
                            System.out.println("Topic name cannot be empty.");
                            break;
                        }

                        System.out.print("Enter category: ");
                        String category = scanner.nextLine().trim();

                        collection.insertOne(
                                new Document("topicName", name)
                                        .append("category", category)
                                        .append("status", "Not Started")
                        );

                        System.out.println("Topic added successfully!");
                        break;

                    case "2":
                        boolean found = false;

                        for (Document doc : collection.find()) {
                            found = true;
                            System.out.println("\nID: " + doc.getObjectId("_id"));
                            System.out.println("Topic: " + doc.getString("topicName"));
                            System.out.println("Category: " + doc.getString("category"));
                            System.out.println("Status: " + doc.getString("status"));
                        }

                        if (!found) {
                            System.out.println("No topics found.");
                        }
                        break;

                    case "3":
                        System.out.print("Enter exact topic name: ");
                        String updateName = scanner.nextLine().trim();

                        Document existing = collection.find(
                                new Document("topicName", updateName)
                        ).first();

                        if (existing == null) {
                            System.out.println("Topic not found.");
                            break;
                        }

                        System.out.println("1. Not Started");
                        System.out.println("2. In Progress");
                        System.out.println("3. Completed");
                        System.out.print("Choose new status: ");

                        String statusChoice = scanner.nextLine().trim();
                        String status;

                        switch (statusChoice) {
                            case "1": status = "Not Started"; break;
                            case "2": status = "In Progress"; break;
                            case "3": status = "Completed"; break;
                            default:
                                System.out.println("Invalid status choice.");
                                break;
                        }

                        if (!statusChoice.matches("[1-3]")) {
                            break;
                        }

                        status = statusChoice.equals("1") ? "Not Started"
                                : statusChoice.equals("2") ? "In Progress"
                                : "Completed";

                        collection.updateOne(
                                new Document("_id", existing.getObjectId("_id")),
                                new Document("$set", new Document("status", status))
                        );

                        System.out.println("Status updated successfully!");
                        break;

                    case "4":
                        System.out.print("Enter exact topic name to delete: ");
                        String deleteName = scanner.nextLine().trim();

                        Document toDelete = collection.find(
                                new Document("topicName", deleteName)
                        ).first();

                        if (toDelete == null) {
                            System.out.println("Topic not found.");
                            break;
                        }

                        System.out.print("Confirm delete? (yes/no): ");
                        String confirm = scanner.nextLine().trim();

                        if (confirm.equalsIgnoreCase("yes")) {
                            DeleteResult result = collection.deleteOne(
                                    new Document("_id", toDelete.getObjectId("_id"))
                            );

                            if (result.getDeletedCount() > 0) {
                                System.out.println("Topic deleted successfully!");
                            } else {
                                System.out.println("Topic was not deleted.");
                            }
                        } else {
                            System.out.println("Delete cancelled.");
                        }
                        break;

                    case "5":
                        System.out.println("Thank you!");
                        return;

                    default:
                        System.out.println("Invalid option. Try again.");
                }
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
