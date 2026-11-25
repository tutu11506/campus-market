import request from './request'

export const getDonationOverview = () => request.get('/donation/overview')

export const createDonation = data => request.post('/donation/records', data)

export const getMyDonations = (params = {}) => request.get('/donation/my', { params })

export const cancelDonation = id => request.post(`/donation/records/${id}/cancel`)
