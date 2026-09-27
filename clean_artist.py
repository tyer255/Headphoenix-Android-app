with open("app/src/main/java/com/example/ArtistScreen.kt", "r") as f:
    content = f.read()

# Split by "Singles & EPs" section end which is probably some closing brackets.
# Instead, let's just find the first "                    // Albums Section" and cut everything after it, then append ONE correct block.

idx = content.find("                    // Albums Section")
if idx != -1:
    clean_content = content[:idx]
    
    albums_and_singles = """                    // Albums Section
                    if (!artist.albums.isNullOrEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Popular Releases",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(artist.albums) { album ->
                                    Column(
                                        modifier = Modifier.width(140.dp).clickable { onNavigateToAlbum(album.id) }
                                    ) {
                                        AsyncImage(
                                            model = album.images?.medium ?: album.images?.small,
                                            contentDescription = album.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(140.dp)
                                                .background(Color.DarkGray)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = album.name,
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${album.year ?: ""} • Album",
                                            color = Color.Gray,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Singles Section
                    if (!artist.singles.isNullOrEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Singles & EPs",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(artist.singles) { single ->
                                    Column(
                                        modifier = Modifier.width(140.dp).clickable { onNavigateToAlbum(single.id) }
                                    ) {
                                        AsyncImage(
                                            model = single.images?.medium ?: single.images?.small,
                                            contentDescription = single.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(140.dp)
                                                .background(Color.DarkGray)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = single.name,
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${single.year ?: ""} • Single",
                                            color = Color.Gray,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
"""
    with open("app/src/main/java/com/example/ArtistScreen.kt", "w") as f:
        f.write(clean_content + albums_and_singles)
