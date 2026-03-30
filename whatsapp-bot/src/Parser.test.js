const { parseMessage } = require('./Parser');

describe('parseMessage()', () => {

    describe('atividades de ESTUDO', () => {
        test('deve reconhecer "estudei 2h python"', () => {
            const result = parseMessage('estudei 2h python');
            expect(result.type).toBe('ACTIVITY');
            expect(result.category).toBe('ESTUDO');
            expect(result.durationMinutes).toBe(120);
        });

        test('deve reconhecer "estudei 1h30 java"', () => {
            const result = parseMessage('estudei 1h30 java');
            expect(result.type).toBe('ACTIVITY');
            expect(result.category).toBe('ESTUDO');
            expect(result.durationMinutes).toBe(90);
        });

        test('deve reconhecer "estudei 45min"', () => {
            const result = parseMessage('estudei 45min');
            expect(result.durationMinutes).toBe(45);
        });
    });

    describe('atividades de TREINO', () => {
        test('deve reconhecer "treinei 45min"', () => {
            const result = parseMessage('treinei 45min');
            expect(result.type).toBe('ACTIVITY');
            expect(result.category).toBe('TREINO');
            expect(result.durationMinutes).toBe(45);
        });

        test('deve reconhecer "malhe 1h"', () => {
            const result = parseMessage('malhe 1h');
            expect(result.category).toBe('TREINO');
            expect(result.durationMinutes).toBe(60);
        });
    });

    describe('atividades de LEITURA', () => {
        test('deve reconhecer "li 30 minutos"', () => {
            const result = parseMessage('li 30 minutos');
            expect(result.type).toBe('ACTIVITY');
            expect(result.category).toBe('LEITURA');
            expect(result.durationMinutes).toBe(30);
        });
    });

    describe('comandos', () => {
        test('deve reconhecer "resumo" como STATS', () => {
            const result = parseMessage('resumo');
            expect(result.type).toBe('COMMAND');
            expect(result.command).toBe('STATS');
        });

        test('deve reconhecer "metas" como GOALS', () => {
            const result = parseMessage('metas');
            expect(result.type).toBe('COMMAND');
            expect(result.command).toBe('GOALS');
        });

        test('deve reconhecer "ajuda" como HELP', () => {
            const result = parseMessage('ajuda');
            expect(result.type).toBe('COMMAND');
            expect(result.command).toBe('HELP');
        });

        test('deve reconhecer "login email@email.com" como LOGIN', () => {
            const result = parseMessage('login email@email.com');
            expect(result.type).toBe('COMMAND');
            expect(result.command).toBe('LOGIN');
        });
    });

    describe('mensagens desconhecidas', () => {
        test('deve retornar UNKNOWN para mensagem sem contexto', () => {
            const result = parseMessage('olá tudo bem');
            expect(result.type).toBe('UNKNOWN');
        });

        test('deve retornar UNKNOWN para string vazia', () => {
            const result = parseMessage('');
            expect(result.type).toBe('UNKNOWN');
        });

        test('deve retornar UNKNOWN para null', () => {
            const result = parseMessage(null);
            expect(result.type).toBe('UNKNOWN');
        });
    });
});
