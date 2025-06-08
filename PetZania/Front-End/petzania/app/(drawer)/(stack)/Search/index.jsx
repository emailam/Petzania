import { StyleSheet, Text, View, TextInput, FlatList, TouchableOpacity } from 'react-native'
import React, { useState } from 'react'
import { Ionicons } from '@expo/vector-icons'
import { useRouter } from 'expo-router'

const MOCK_DATA = [
    'Golden Retriever',
    'Persian Cat',
    'Parrot',
    'Fish',
    'Sarah Wilson',
    'Dog Training',
    'Cat Adoption',
    'Emily Chen',
    'Buddy',
    'Luna',
]

export default function SearchScreen() {
    const [query, setQuery] = useState('')
    const [results, setResults] = useState([])
    const router = useRouter()

    const handleSearch = (text) => {
        setQuery(text)
        if (text.trim() === '') {
            setResults([])
        } else {
            setResults(
                MOCK_DATA.filter(item => item.toLowerCase().includes(text.toLowerCase()))
            )
        }
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
                        placeholder="Search..."
                        value={query}
                        onChangeText={handleSearch}
                    />
                </View>
            </View>
            <FlatList
                data={results}
                keyExtractor={(item, idx) => idx.toString()}
                renderItem={({ item }) => (
                    <TouchableOpacity style={styles.resultItem}>
                        <Text style={styles.resultText}>{item}</Text>
                    </TouchableOpacity>
                )}
                ListEmptyComponent={query ? <Text style={styles.noResults}>No results found</Text> : null}
            />
        </View>
    )
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#fff',
        padding: 16,
        paddingTop: 40, // Adjust for status bar height
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
        borderColor: '#ccc',
        borderRadius: 8,
        paddingHorizontal: 8,
        paddingVertical: 2,
    },
    input: {
        flex: 1,
        fontSize: 16,
        padding: 10,
        color: '#333',
        borderWidth: 0,
        backgroundColor: 'transparent',
    },
    resultItem: {
        padding: 12,
        borderBottomWidth: 1,
        borderBottomColor: '#eee',
    },
    resultText: {
        fontSize: 16,
    },
    noResults: {
        textAlign: 'center',
        color: '#888',
        marginTop: 24,
    },
})