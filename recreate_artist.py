import os

with open("app/src/main/java/com/example/ArtistScreen.kt", "r") as f:
    lines = f.readlines()

# find the FIRST itemsIndexed (artist.topTracks
idx = -1
for i, line in enumerate(lines):
    if "itemsIndexed( (artist.topTracks ?: emptyList()) )" in line:
        idx = i
        break

# The loop ends 34 lines after idx typically.
# Let's just truncate the file at the first "                }\n            }\n        }\n    }\n}" after idx.

end_idx = -1
for i in range(idx, len(lines)):
    if "TrackMenuState(track)" in lines[i]:
        # skip next few lines
        end_idx = i + 10
        break

new_lines = lines[:end_idx]
new_lines.extend([
    "                    }\n",
    "                    // Albums Section\n",
    "                    if (!artist.albums.isNullOrEmpty()) {\n",
    "                        item {\n",
    "                            Spacer(modifier = Modifier.height(24.dp))\n",
    "                            Text(\n",
    "                                text = \"Popular Releases\",\n",
    "                                style = MaterialTheme.typography.titleLarge,\n",
    "                                fontWeight = FontWeight.Bold,\n",
    "                                color = Color.White,\n",
    "                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)\n",
    "                            )\n",
    "                            androidx.compose.foundation.lazy.LazyRow(\n",
    "                                horizontalArrangement = Arrangement.spacedBy(16.dp),\n",
    "                                contentPadding = PaddingValues(horizontal = 16.dp)\n",
    "                            ) {\n",
    "                                items(artist.albums) { album ->\n",
    "                                    Column(\n",
    "                                        modifier = Modifier.width(140.dp)\n",
    "                                    ) {\n",
    "                                        AsyncImage(\n",
    "                                            model = album.images?.medium ?: album.images?.small,\n",
    "                                            contentDescription = album.name,\n",
    "                                            contentScale = ContentScale.Crop,\n",
    "                                            modifier = Modifier\n",
    "                                                .size(140.dp)\n",
    "                                                .background(Color.DarkGray)\n",
    "                                        )\n",
    "                                        Spacer(modifier = Modifier.height(8.dp))\n",
    "                                        Text(\n",
    "                                            text = album.name,\n",
    "                                            color = Color.White,\n",
    "                                            style = MaterialTheme.typography.bodyMedium,\n",
    "                                            fontWeight = FontWeight.Bold,\n",
    "                                            maxLines = 1,\n",
    "                                            overflow = TextOverflow.Ellipsis\n",
    "                                        )\n",
    "                                        Text(\n",
    "                                            text = \"${album.year ?: \"\"} • Album\",\n",
    "                                            color = Color.Gray,\n",
    "                                            style = MaterialTheme.typography.bodySmall,\n",
    "                                            maxLines = 1,\n",
    "                                            overflow = TextOverflow.Ellipsis\n",
    "                                        )\n",
    "                                    }\n",
    "                                }\n",
    "                            }\n",
    "                        }\n",
    "                    }\n",
    "                    \n",
    "                    // Singles Section\n",
    "                    if (!artist.singles.isNullOrEmpty()) {\n",
    "                        item {\n",
    "                            Spacer(modifier = Modifier.height(24.dp))\n",
    "                            Text(\n",
    "                                text = \"Singles & EPs\",\n",
    "                                style = MaterialTheme.typography.titleLarge,\n",
    "                                fontWeight = FontWeight.Bold,\n",
    "                                color = Color.White,\n",
    "                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)\n",
    "                            )\n",
    "                            androidx.compose.foundation.lazy.LazyRow(\n",
    "                                horizontalArrangement = Arrangement.spacedBy(16.dp),\n",
    "                                contentPadding = PaddingValues(horizontal = 16.dp)\n",
    "                            ) {\n",
    "                                items(artist.singles) { single ->\n",
    "                                    Column(\n",
    "                                        modifier = Modifier.width(140.dp)\n",
    "                                    ) {\n",
    "                                        AsyncImage(\n",
    "                                            model = single.images?.medium ?: single.images?.small,\n",
    "                                            contentDescription = single.name,\n",
    "                                            contentScale = ContentScale.Crop,\n",
    "                                            modifier = Modifier\n",
    "                                                .size(140.dp)\n",
    "                                                .background(Color.DarkGray)\n",
    "                                        )\n",
    "                                        Spacer(modifier = Modifier.height(8.dp))\n",
    "                                        Text(\n",
    "                                            text = single.name,\n",
    "                                            color = Color.White,\n",
    "                                            style = MaterialTheme.typography.bodyMedium,\n",
    "                                            fontWeight = FontWeight.Bold,\n",
    "                                            maxLines = 1,\n",
    "                                            overflow = TextOverflow.Ellipsis\n",
    "                                        )\n",
    "                                        Text(\n",
    "                                            text = \"${single.year ?: \"\"} • Single\",\n",
    "                                            color = Color.Gray,\n",
    "                                            style = MaterialTheme.typography.bodySmall,\n",
    "                                            maxLines = 1,\n",
    "                                            overflow = TextOverflow.Ellipsis\n",
    "                                        )\n",
    "                                    }\n",
    "                                }\n",
    "                            }\n",
    "                        }\n",
    "                    }\n",
    "                }\n",
    "            }\n",
    "        }\n",
    "    }\n",
    "}\n"
])

with open("app/src/main/java/com/example/ArtistScreen.kt", "w") as f:
    f.writelines(new_lines)
