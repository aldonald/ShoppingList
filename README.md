# ShoppingList
This is the android app for the ShoppingApp

# Alex Donald
# 218362669

## Overview
The project is a shopping list app with an independent storage accessed through APIs. The app allows for smart devices to 
upload shopping items to the server and these will be added to the user's list and a push notification sent to the phone.

An emulator will need to be set up for internet access in order to use correctly.

## Repositories:
There are 2 repositories for this project.

There is the UI Kotlin repository which can be found at https://github.com/aldonald/ShoppingList.

The backend, server and database is written in Python, Django interacting with a PSQL database. This is available at https://github.com/aldonald/shoppingApp.


## Running the project.
The server is being hosted at: https://tranquil-lowlands-73758.herokuapp.com/

Use the above to sign up for an account if you wish to access directly or sign up through the app.
The API endpoint to view ShoppingItems is: https://tranquil-lowlands-73758.herokuapp.com/api/shoppingitems/

The API endpoint to add items from a device is: https://tranquil-lowlands-73758.herokuapp.com/api/pi_add_item/

If you are logged in with the same login as in the app this will create the notification - make sure the app is not open on the device.

The app is registered with Google Play Store as "Shopping List" and is currently "Pending publication". If by the time this is marked, the a
pp has been approved this would be the easiest way to run. The app has the description "Manage and automate your shopping list.". My deakin
email "donaldal@deakin.edu.au" has been given as a contact.

