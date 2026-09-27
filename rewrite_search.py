import re

with open("app/src/main/java/com/example/SearchScreen.kt", "r") as f:
    content = f.read()

find = """        // Search Input Box
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {"""

replace = """        // Search Input Box
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)) // Fully rounded like the web
                    .background(Color(0xFF242424))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Filled.ArrowBack, 
                        contentDescription = "Back", 
                        tint = Color.White, 
                        modifier = Modifier.size(24.dp).clickable { searchViewModel.updateQuery("") }
                    )
                } else {
                    Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                
                BasicTextField(
                    value = query,
                    onValueChange = { searchViewModel.updateQuery(it) },
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    cursorBrush = SolidColor(Color.White),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text("What do you want to listen to?", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                        innerTextField()
                    }
                )
                
                if (query.isEmpty()) {
                    Icon(Icons.Filled.Mic, contentDescription = "Voice", tint = Color.Gray, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Camera", tint = Color.Gray, modifier = Modifier.size(24.dp))
                }
            }
        }
        """

content = content.replace("""            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Filled.ArrowBack, 
                        contentDescription = "Back", 
                        tint = Color.Black, 
                        modifier = Modifier.size(24.dp).clickable { searchViewModel.updateQuery("") }
                    )
                } else {
                    Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.Black, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                
                BasicTextField(
                    value = query,
                    onValueChange = { searchViewModel.updateQuery(it) },
                    textStyle = TextStyle(color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text("What do you want to listen to?", color = Color.DarkGray, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                        innerTextField()
                    }
                )""", "")
content = content.replace(find, replace)
with open("app/src/main/java/com/example/SearchScreen.kt", "w") as f:
    f.write(content)
