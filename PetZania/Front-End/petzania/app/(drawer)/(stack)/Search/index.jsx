import { StyleSheet, Text, View, TextInput, FlatList, TouchableOpacity, Image, ActivityIndicator } from 'react-native'
import React, { useState, useEffect } from 'react'
import { Ionicons } from '@expo/vector-icons'
import { useRouter } from 'expo-router'

import { searchByUsername } from '@/services/searchService'

export default function SearchScreen() {
    const [query, setQuery] = useState('');
    const [results, setResults] = useState([]);
    const [loading, setLoading] = useState(false);
    const [loadingMore, setLoadingMore] = useState(false);
    const [currentPage, setCurrentPage] = useState(0);
    const [hasMore, setHasMore] = useState(false);
    const router = useRouter();

    useEffect(() => {
        const timeoutId = setTimeout(() => {
            if (query.trim() === '') {
                setResults([])
                setLoading(false)
                setCurrentPage(0)
                setHasMore(false)
                return
            }
            performSearch(query, 0, true)
        }, 500)

        return () => clearTimeout(timeoutId)
    }, [query])

    const performSearch = async (searchQuery, page = 0, resetResults = false) => {
        if (resetResults) {
            setLoading(true)
        } else {
            setLoadingMore(true)
        }

        try {
            const searchResults = await searchByUsername(searchQuery, page, 10)
            if (resetResults) {
                setResults(searchResults.content)
                setCurrentPage(0)
            } else {
                setResults(prev => [...prev, ...searchResults.content])
                setCurrentPage(page)
            }

            setHasMore(searchResults.content.length === 10 && !searchResults.last)
        } catch (error) {
            console.error('Search error:', error)
            if (resetResults) {
                setResults([])
            }
            setHasMore(false)
        } finally {
            if (resetResults) {
                setLoading(false)
            } else {
                setLoadingMore(false)
            }
        }
    }

    const loadMoreResults = () => {
        if (!loadingMore && hasMore && query.trim()) {
            performSearch(query, currentPage + 1, false)
        }
    }

    const handleSearch = (text) => {
        setQuery(text)
    }

    return (
        <View style={styles.container}>
            <View style={styles.topBar}>
                <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
                    <Ionicons name="arrow-back" size={24} color="#9188E5" />
                </TouchableOpacity>
                <View style={styles.searchInputContainer}>
                    <TextInput
                        style={styles.input}
                        placeholder="Search users..."
                        value={query}
                        onChangeText={handleSearch}
                        placeholderTextColor="#999"
                        autoFocus={true}
                    />
                    {query.length > 0 && (
                        <TouchableOpacity
                            onPress={() => setQuery('')}
                            style={styles.clearButton}
                        >
                            <Ionicons name="close-circle" size={20} color="#ccc" />
                        </TouchableOpacity>
                    )}
                </View>
            </View>
            <FlatList
                data={results}
                keyExtractor={(item) => {
                    return item.userId.toString()
                }}
                renderItem={({ item }) => (
                    <TouchableOpacity
                        style={styles.resultItem}
                        onPress={() => {
                            console.log('Selected user:', item)
                            router.push(`/UserModule/${item.userId}`)
                        }}
                        activeOpacity={0.7}
                    >
                        <View style={styles.profileImageContainer}>
                            {item.profilePictureURL ? (
                                <Image
                                    source={{ uri: item.profilePictureURL }}
                                    style={styles.profileImage}
                                />
                            ) : (
                                <View style={styles.profileImage}>
                                    <Ionicons name="person" size={24} color="#9188E5" />
                                </View>
                            )}
                        </View>

                        <View style={styles.userInfoContainer}>
                            <Text style={styles.resultText}>
                                {item.username || item.name || 'Unknown User'}
                            </Text>
                        </View>

                        <Ionicons name="chevron-forward" size={20} color="#ccc" />
                    </TouchableOpacity>
                )}
                ListEmptyComponent={
                    loading ? (
                        <View style={styles.loadingContainer}>
                            <ActivityIndicator size="large" color="#9188E5" />
                            <Text style={styles.loadingText}>Searching users...</Text>
                        </View>
                    ) : query.trim() ? (
                        <View style={styles.emptyContainer}>
                            <Ionicons name="search" size={60} color="#ddd" />
                            <Text style={styles.noResults}>No users found</Text>
                            <Text style={styles.noResultsSubtext}>Try searching with a different username</Text>
                        </View>
                    ) : (
                        <View style={styles.emptyContainer}>
                            <Ionicons name="people" size={60} color="#ddd" />
                            <Text style={styles.noResults}>Search for users</Text>
                            <Text style={styles.noResultsSubtext}>Start typing to find other pet owners</Text>
                        </View>
                    )
                }
                ListFooterComponent={
                    hasMore && results.length >= 10 ? (
                        <TouchableOpacity
                            style={styles.showMoreButton}
                            onPress={loadMoreResults}
                            disabled={loadingMore}
                        >
                            {loadingMore ? (
                                <ActivityIndicator size="small" color="#9188E5" />
                            ) : (
                                <Text style={styles.showMoreText}>Show more</Text>
                            )}
                        </TouchableOpacity>
                    ) : null
                }
            />
        </View>
    )
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#fff',
        padding: 16,
        paddingTop: 40,
    },
    topBar: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 16,
    },
    backButton: {
        marginRight: 8,
        padding: 4,
    },
    searchInputContainer: {
        flex: 1,
        flexDirection: 'row',
        alignItems: 'center',
        borderWidth: 1,
        borderColor: '#e0e0e0',
        borderRadius: 12,
        paddingHorizontal: 12,
        paddingVertical: 8,
        backgroundColor: '#f8f9fa',
    },
    input: {
        flex: 1,
        fontSize: 16,
        color: '#333',
        paddingVertical: 4,
    },
    clearButton: {
        marginLeft: 8,
        padding: 2,
    },
    resultItem: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: 12,
        borderBottomWidth: 1,
        borderBottomColor: '#f0f0f0',
        backgroundColor: '#fff',
    },
    profileImageContainer: {
        marginRight: 12,
    },
    profileImage: {
        width: 50,
        height: 50,
        borderRadius: 25,
        borderWidth: 1,
        borderColor: '#9188E5',
        justifyContent: 'center',
        alignItems: 'center',
    },
    userInfoContainer: {
        flex: 1,
        marginRight: 8,
    },
    resultText: {
        fontSize: 16,
        fontWeight: '600',
        color: '#333',
        marginBottom: 2,

    },
    resultSubText: {
        fontSize: 14,
        color: '#666',
        marginBottom: 1,
    },
    mailText: {
        fontSize: 12,
        color: '#9188E5',
        fontStyle: 'italic',
    },
    loadingContainer: {
        alignItems: 'center',
        marginTop: 60,
    },
    loadingText: {
        marginTop: 12,
        fontSize: 16,
        color: '#9188E5',
    },
    emptyContainer: {
        alignItems: 'center',
        marginTop: 60,
        paddingHorizontal: 40,
    },
    noResults: {
        textAlign: 'center',
        color: '#666',
        marginTop: 16,
        fontSize: 18,
        fontWeight: '600',
    },    noResultsSubtext: {
        textAlign: 'center',
        color: '#999',
        marginTop: 8,
        fontSize: 14,
    },
    showMoreButton: {
        backgroundColor: '#9188E5',
        marginHorizontal: 16,
        marginVertical: 16,
        paddingVertical: 12,
        paddingHorizontal: 24,
        borderRadius: 8,
        alignItems: 'center',
        justifyContent: 'center',
    },
    showMoreText: {
        color: '#fff',
        fontSize: 16,
        fontWeight: '600',
    },
})