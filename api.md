# API

There is a basic REST api for getting machine-parsable data about menus.
Everything is accessed based on the date.
The base URL for the api is

```
http://menu.yancey.io/api/v1/${ISO-8601 date}
```

A 404 response means that the requested data is not available.
This usually means that either there's no data for that day,
or that there was an unexpected error fetching the data for that day.

The API has a hierarchical structure, going

```
dining hall -> meal -> station -> items
```

## API Nodes

The API always returns JSON objects or arrays.
Below, things that don't follow the JSON spec (unquoted, ..., etc)
are used to denote things that will be filled in by the API.

<!-- (not necessarily true)
Replacing any of the URL parameters (denoted `${someting}`) except the date with
"all" will return a JSON object consisting of all of the options,
keyed by whatever they're keyed by in the API.
-->

### Dining halls
```
http://menu.yancey.io/api/v1/${ISO-8601 date}/dining-halls
```
Returns a list of dining hall descriptions,

```
[
	{
		"id": the internal dining hall id,
		"name": a user-friendly name for the dining hall
		"url": a link to the dining hall's official menu on that day
	},
	...
]
```

### Meals
```
http://menu.yancey.io/api/v1/${ISO-8601 date}/${dining-hall-id}/meals
```
Returns an array of meal descriptions,

```
[
	{
		"id": the internal meal id,
		"name": a user-friendly name for the meal,
		"description": any special description the meal might have
		               (can be blank),
		"startTime": the meal's start time, formatted hh:mm (24-hour time),
		"endTime": the meal's end time, formatted hh:mm (24-hour time)
	},
	...
]
```

The `startTime` and `endTime` may be omitted if they are not available.

### Stations

```
http://menu.yancey.io/api/v1/${ISO-8601 date}/${dining-hall-id}/${meal-id}/stations
```
Returns an array of station descriptions,

```
[
	{
		"id": the internal station id,
		"name": a user-friendly name for the station
	},
	...
]
```

### Items

```
http://menu.yancey.io/api/v1/${ISO-8601 date}/${dining-hall-id}/${meal-id}/${station-id}/items
```
Returns an array of item descriptions

```
[
	{
		"name": the item's name,
		"description": the item's description (sometimes the same as the name),
		"tags": [
			A list of tags (as strings),
			such as "gluten free" or "vegetarian"
		]
	},
	...
]
```
### All
At any point along the hierarchy (after the date), the API node can be replaced with "all", which will cause all the nested data to be returned at once, nested in the JSON with its API keys.

For example, a request to

```
http://menu.yancey.io/api/v1/2016-09-16/hoch/lunch/all
```
would return all of the stations and items for lunch at the hoch that day, formatted as

```
[
	{
		"id": "exhibition",
		"name": "Exhibition",
		"items": [
			{
				"name": "Stir-fry",
				"description": "Chicken and tofu stir-fry",
				"tags": [
					"vegetarian"
				]
			},
			{
				"name": "Jasmine rice",
				"description": "for some reason, this is listed seperately...",
				"tags": [
					"vegetarian"
				]
			},
			...
		]
	},
	{
		"id": "creations",
		"name": "Creations",
		"items": [
			{
				"name": "Make your own salad",
				"description": "",
				"tags": [
					"vegetarian",
					"gluten free"
				]
			},
			...
		]
	},
	...
]
```
Note that `"items"` was addded to each station with a list of the items it serves.
