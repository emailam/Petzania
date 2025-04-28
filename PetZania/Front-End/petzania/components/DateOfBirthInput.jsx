import React, { useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Platform } from 'react-native';
import DateTimePicker from '@react-native-community/datetimepicker';

export default function DateOfBirthInput({ label = "Date of Birth", value, onChange, errorMessage }) {
    const [open, setOpen] = useState(false);
    const [selectedDate, setSelectedDate] = useState(value ? new Date(value) : new Date());

    const formatDate = (date) => {
        const year = date.getFullYear();
        const month = (date.getMonth() + 1).toString().padStart(2, '0');
        const day = date.getDate().toString().padStart(2, '0');
        return `${year}-${month}-${day}`;
    };

    return (
        <View style={styles.inputContainer}>
            <Text style={styles.label}>
                {label}
                <Text style={{ fontSize: 18, color: 'red' }}>*</Text>
            </Text>

            <TouchableOpacity
                style={styles.input}
                onPress={() => setOpen(true)}
            >
                <Text style={{ fontSize: 16, color: value ? '#000' : '#aaa' }}>
                    {value ? value : 'Select date'}
                </Text>
            </TouchableOpacity>

            {errorMessage ? <Text style={styles.errorText}>{errorMessage}</Text> : null}

            {open && (
                <DateTimePicker
                    value={selectedDate}
                    mode="date"
                    display="default"
                    maximumDate={new Date()}
                    onChange={(event, date) => {
                        setOpen(false);
                        if (date) {
                            setSelectedDate(date);
                            const formatted = formatDate(date);
                            onChange(formatted);
                        }
                    }}
                />
            )}
        </View>
    );
}

const styles = StyleSheet.create({
    inputContainer: {
        width: '100%',
    },
    label: {
        fontSize: 18,
        fontWeight: 'bold',
        marginBottom: 10,
        color: '#333',
    },
    input: {
        width: '100%',
        height: 50,
        borderWidth: 1,
        borderColor: '#9188E5',
        borderRadius: 10,
        paddingHorizontal: 15,
        justifyContent: 'center',
        backgroundColor: '#fff',
    },
    errorText: {
        color: 'red',
        marginTop: 5,
    },
});
